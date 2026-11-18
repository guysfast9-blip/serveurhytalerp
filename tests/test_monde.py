from atelier.monde import LIEUX, RIVIERES, LACS, ROUTES, LARGEUR, HAUTEUR, Lieu


def compte(t):
    return sum(1 for l in LIEUX if l.type == t)


def test_inventaire_des_lieux():
    assert (compte("capitale"), compte("ville"), compte("village"), compte("lieu")) == (5, 1, 13, 4)


def test_noms_uniques():
    noms = [l.nom for l in LIEUX]
    assert len(noms) == len(set(noms))


def test_conversion_des_coordonnees():
    l = Lieu("Test", "village", 0.5, 0.5, 15)
    assert l.pixel() == (2400, 1800) and l.bloc() == (0, 0)


def test_tout_est_dans_la_carte():
    for l in LIEUX:
        x, z = l.pixel()
        assert 0 <= x < LARGEUR and 0 <= z < HAUTEUR


def test_routes_referencent_des_lieux_existants():
    noms = {l.nom for l in LIEUX}
    for route in ROUTES:
        for p in route:
            assert not isinstance(p, str) or p in noms


def test_eau_decrite():
    assert len(RIVIERES) == 5 and len(LACS) == 4
