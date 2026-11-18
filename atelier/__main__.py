"""python3 -m atelier <dossier> : génère le monde, exporte les cartes de données et un aperçu."""
import sys
import time
from pathlib import Path

from .export import apercu, exporter, generer_monde


def main() -> None:
    dossier = Path(sys.argv[1] if len(sys.argv) > 1 else "donnees")
    debut = time.time()
    monde = generer_monde()
    exporter(monde, dossier)
    apercu(monde, dossier / "apercu.png")
    print(f"Monde d'Aldermyr exporté dans {dossier}/ en {time.time() - debut:.0f} s")


if __name__ == "__main__":
    main()
