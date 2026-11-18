import pytest


@pytest.fixture(scope="session")
def monde():
    from atelier.export import generer_monde
    return generer_monde()
