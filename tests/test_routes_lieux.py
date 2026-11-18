import numpy as np
import pytest
from scipy.ndimage import label

from atelier.altitude import carte_altitude
from atelier.eau import appliquer_eau, trace_riviere
from atelier.lieux import aplanir_lieux, lieux_json
from atelier.monde import LIEUX, RIVIERES, ROUTES, Region
from atelier.regions import carte_regions
from atelier.routes import carte_routes


@pytest.fixture(scope="module")
def cartes():
    reg = carte_regions()
    alt, niv = appliquer_eau(carte_altitude(reg), reg)
    alt = aplanir_lieux(alt, niv)
    return reg, alt, niv, carte_routes(alt, niv)


def test_lieux_plats(cartes):
    reg, alt, niv, rou = cartes
    for l in LIEUX:
        if l.rayon == 0:
            continue
        x, z = l.pixel()
        r = l.rayon // 2
        assert np.ptp(alt[z - r:z + r + 1, x - r:x + r + 1]) <= 1, l.nom


def test_aucun_lieu_dans_l_eau(cartes):
    reg, alt, niv, rou = cartes
    for l in LIEUX:
        x, z = l.pixel()
        assert np.isnan(niv[z, x]), l.nom


def test_port_ecume_sur_la_cote(cartes):
    reg, alt, niv, rou = cartes
    x, z = next(l for l in LIEUX if l.nom == "Port-Écume").pixel()
    assert reg[z, x] == Region.COTE


def test_routes_relient_leurs_extremites(cartes):
    reg, alt, niv, rou = cartes
    lab, _ = label(rou, structure=np.ones((3, 3)))
    pos = {l.nom: l.pixel() for l in LIEUX}
    for route in ROUTES:
        (xa, za), (xb, zb) = pos[route[0]], pos[route[-1]]
        assert lab[za, xa] != 0 and lab[za, xa] == lab[zb, xb], route


def test_routes_jamais_sur_lac_ni_mer(cartes):
    reg, alt, niv, rou = cartes
    riv = np.zeros_like(rou)
    for r in RIVIERES:
        for x, z in trace_riviere(r):
            riv[max(z - 15, 0):z + 16, max(x - 15, 0):x + 16] = True
    eau_interdite = ~np.isnan(niv) & ~riv
    assert not (rou & eau_interdite).any()


def test_lieux_json():
    d = lieux_json()
    assert len(d) == 23 and set(d[0]) == {"nom", "type", "x", "z", "rayon"}
    a = next(e for e in d if e["nom"] == "Aurelia")
    assert (a["x"], a["z"]) == (0, 0) and a["rayon"] == 60


def test_routes_ne_traversent_aucun_lac(cartes):
    from atelier.eau import masques_lacs
    reg, alt, niv, rou = cartes
    for m in masques_lacs(reg):
        assert not (rou & m).any()
