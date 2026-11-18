import json

import numpy as np
import pytest
from PIL import Image

from atelier.export import Monde, apercu, exporter, lire


def test_fichiers_et_formats(monde, tmp_path):
    exporter(monde, tmp_path)
    for nom, mode in [("altitude.png", "I;16"), ("eau.png", "I;16"), ("regions.png", "L"), ("routes.png", "L")]:
        im = Image.open(tmp_path / nom)
        assert im.size == (4800, 3600) and im.mode == mode, nom
    assert len(json.loads((tmp_path / "lieux.json").read_text(encoding="utf-8"))) == 23


def test_aller_retour(monde, tmp_path):
    exporter(monde, tmp_path)
    m = lire(tmp_path)
    assert np.array_equal(m.altitude, np.round(monde.altitude))
    assert np.array_equal(m.regions, monde.regions) and np.array_equal(m.routes, monde.routes)
    assert np.array_equal(np.isnan(m.niveau), np.isnan(monde.niveau))


def test_valeurs_extremes_encodees(tmp_path):
    z = np.zeros((3600, 4800), np.float32)
    alt = z.copy()
    alt[0, 0], alt[0, 1] = -40, 180
    niv = np.full_like(z, np.nan)
    niv[0, 0] = 0
    m = Monde(alt, np.zeros_like(z, np.uint8), niv, np.zeros_like(z, bool), [])
    exporter(m, tmp_path)
    r = lire(tmp_path)
    assert (r.altitude[0, 0], r.altitude[0, 1], r.niveau[0, 0]) == (-40, 180, 0)


def test_debordement_refuse(tmp_path):
    z = np.zeros((3600, 4800), np.float32)
    z[0, 0] = -2000
    m = Monde(z, np.zeros_like(z, np.uint8), np.full_like(z, np.nan), np.zeros_like(z, bool), [])
    with pytest.raises(ValueError):
        exporter(m, tmp_path)


def test_apercu(monde, tmp_path):
    apercu(monde, tmp_path / "apercu.png")
    assert Image.open(tmp_path / "apercu.png").size == (1600, 1200)
