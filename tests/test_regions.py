import numpy as np
import pytest

from atelier.bruit import fbm
from atelier.monde import LIEUX, Region
from atelier.regions import carte_regions


@pytest.fixture(scope="module")
def reg():
    return carte_regions()


def region_de(reg, nom):
    x, z = next(l for l in LIEUX if l.nom == nom).pixel()
    return Region(reg[z, x])


def test_fbm_deterministe_et_borne():
    a, b = fbm(64, 80, 4, 4, 3), fbm(64, 80, 4, 4, 3)
    assert a.dtype == np.float32 and np.array_equal(a, b) and a.min() >= 0 and a.max() <= 1


def test_forme(reg):
    assert reg.shape == (3600, 4800) and reg.dtype == np.uint8


@pytest.mark.parametrize("nom,region", [
    ("Aurelia", Region.PLAINES), ("Durmarak", Region.MONTAGNES), ("Cœur-Sylve", Region.FORET),
    ("Gorrath", Region.STEPPES), ("Valmont", Region.PLAINES), ("Carrefour des Pendus", Region.MARCHES)])
def test_lieux_dans_leur_region(reg, nom, region):
    assert region_de(reg, nom) == region


def test_centre_du_cratere(reg):
    assert reg[int(0.36 * 3600), int(0.50 * 4800)] == Region.CRATERE


def test_le_monde_finit_en_mer(reg):
    b = 30
    assert (reg[:b] == 0).all() and (reg[-b:] == 0).all() and (reg[:, :b] == 0).all() and (reg[:, -b:] == 0).all()


def test_frontiere_plaines_steppes_ondule(reg):
    xs = []
    for z in range(int(0.25 * 3600), int(0.55 * 3600), 10):
        ligne = reg[z, :2400]
        st = np.where(ligne == Region.STEPPES)[0]
        if st.size:
            xs.append(st.max())
    assert max(xs) - min(xs) >= 30
