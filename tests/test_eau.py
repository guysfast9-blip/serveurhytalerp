import numpy as np
import pytest

from atelier.altitude import carte_altitude
from atelier.eau import appliquer_eau, trace_riviere
from atelier.monde import LACS, RIVIERES, Region
from atelier.regions import carte_regions


@pytest.fixture(scope="module")
def cartes():
    reg = carte_regions()
    alt, niv = appliquer_eau(carte_altitude(reg), reg)
    return reg, alt, niv


def test_mer_au_niveau_zero(cartes):
    reg, alt, niv = cartes
    assert np.all(niv[reg == Region.MER] == 0)


def test_lac_plat(cartes):
    reg, alt, niv = cartes
    for lac in LACS:
        x, z = round(lac.u * 4800), round(lac.v * 3600)
        zone = niv[z - 20:z + 21, x - 20:x + 21]
        assert not np.isnan(zone).any() and np.ptp(zone) == 0, lac.nom


def test_rivieres_descendent_vers_l_aval(cartes):
    reg, alt, niv = cartes
    for r in RIVIERES:
        pts = trace_riviere(r)
        n = np.array([niv[z, x] for x, z in pts if not np.isnan(niv[z, x])])
        assert len(n) > 0.9 * len(pts) and np.all(np.diff(n) <= 1e-3), r.nom


def test_grand_fleuve_atteint_la_mer(cartes):
    reg, alt, niv = cartes
    x, z = trace_riviere(RIVIERES[0])[-1]
    assert niv[z, x] == 0


def test_lit_sous_la_surface(cartes):
    reg, alt, niv = cartes
    eau = ~np.isnan(niv)
    assert np.all(alt[eau] < niv[eau])


def test_cratere_a_sec(cartes):
    reg, alt, niv = cartes
    assert np.isnan(niv[np.isin(reg, [Region.CRATERE, Region.RUINES])]).all()
