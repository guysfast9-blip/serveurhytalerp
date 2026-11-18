import numpy as np
import pytest
from scipy.ndimage import binary_erosion

from atelier.altitude import carte_altitude
from atelier.monde import ALTITUDES, LIEUX, Region
from atelier.regions import carte_regions


@pytest.fixture(scope="module")
def cartes():
    reg = carte_regions()
    return reg, carte_altitude(reg)


def test_mer_sous_l_eau_et_profondeur(cartes):
    reg, alt = cartes
    mer = alt[reg == Region.MER]
    assert mer.max() <= -5 + 0.5 and mer.min() >= -40 - 0.5


def test_terres_hors_mer_au_dessus_de_zero(cartes):
    reg, alt = cartes
    terre = (reg != Region.MER) & (reg != Region.CRATERE)
    assert alt[terre].min() > 0


def test_bornes_par_region(cartes):
    # les cœurs de région (loin des frontières) respectent le tableau, à 2 blocs près
    reg, alt = cartes
    for r, (lo, hi) in ALTITUDES.items():
        coeur = binary_erosion(reg == r, iterations=80)
        x, z = next(l for l in LIEUX if l.nom == "Mont Braise").pixel()
        coeur[max(z - 120, 0):z + 121, max(x - 120, 0):x + 121] = False   # le cône dépasse exprès
        if coeur.any():
            v = alt[coeur]
            assert v.min() >= lo - 2 and v.max() <= hi + 2, r.name


def test_fond_du_cratere(cartes):
    reg, alt = cartes
    assert -31 <= alt[int(0.36 * 3600), int(0.50 * 4800)] <= -25


def test_mont_braise_domine_les_steppes(cartes):
    reg, alt = cartes
    x, z = next(l for l in LIEUX if l.nom == "Mont Braise").pixel()
    assert alt[z, x] >= 55


def test_pas_de_falaise_hors_montagnes(cartes):
    reg, alt = cartes
    dz = np.maximum(np.abs(np.diff(alt, axis=0))[:, :-1], np.abs(np.diff(alt, axis=1))[:-1, :])
    doux = np.isin(reg[:-1, :-1], [Region.PLAINES, Region.COTE, Region.MARCHES, Region.FORET])
    # le passage terre → mer est une marche sous l'eau (mer à −5 dès le rivage) : on l'exclut
    mer = reg == Region.MER
    touche_mer = mer[:-1, :-1] | mer[1:, :-1] | mer[:-1, 1:]
    assert dz[doux & ~touche_mer].max() <= 3
