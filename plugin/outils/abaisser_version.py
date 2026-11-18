"""Copie un JAR en ramenant la version des .class à Java 21 (65), pour compiler avec javac 21.
Le JAR produit sert UNIQUEMENT à la compilation ; il n'est jamais exécuté."""
import sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as zi, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zo:
    for info in zi.infolist():
        data = zi.read(info)
        if info.filename.endswith(".class") and data[:4] == b"\xca\xfe\xba\xbe" and int.from_bytes(data[6:8], "big") > 65:
            data = data[:6] + (65).to_bytes(2, "big") + data[8:]
        zo.writestr(info, data)
