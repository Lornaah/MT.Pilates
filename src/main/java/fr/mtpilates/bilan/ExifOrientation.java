package fr.mtpilates.bilan;

/**
 * Lecture de l'orientation EXIF d'un JPEG (balise 0x0112). Les téléphones enregistrent souvent la photo couchée avec
 * cette balise ; le navigateur la redresse, Java2D non : sans elle, les annotations du coach seraient décalées.
 */
final class ExifOrientation {

    private static final int NORMAL = 1;
    private static final int ORIENTATION_TAG = 0x0112;

    private ExifOrientation() {
    }

    static int of(final byte[] jpeg) {
        if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
            return NORMAL;
        }
        int offset = 2;
        while (offset + 4 < jpeg.length && (jpeg[offset] & 0xFF) == 0xFF) {
            final int marker = jpeg[offset + 1] & 0xFF;
            final int length = u16(jpeg, offset + 2, false);
            if (marker == 0xE1 && isExif(jpeg, offset + 4)) {
                return fromTiff(jpeg, offset + 10);
            }
            if (marker == 0xDA) {
                break;
            }
            offset += 2 + length;
        }
        return NORMAL;
    }

    private static boolean isExif(final byte[] b, final int at) {
        return at + 6 <= b.length && b[at] == 'E' && b[at + 1] == 'x' && b[at + 2] == 'i' && b[at + 3] == 'f';
    }

    private static int fromTiff(final byte[] b, final int tiff) {
        if (tiff + 8 > b.length) {
            return NORMAL;
        }
        final boolean little = b[tiff] == 'I';
        final int ifd = tiff + (int) u32(b, tiff + 4, little);
        if (ifd + 2 > b.length) {
            return NORMAL;
        }
        final int entries = u16(b, ifd, little);
        for (int i = 0; i < entries; i++) {
            final int entry = ifd + 2 + i * 12;
            if (entry + 12 > b.length) {
                break;
            }
            if (u16(b, entry, little) == ORIENTATION_TAG) {
                final int value = u16(b, entry + 8, little);
                return value >= 1 && value <= 8 ? value : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int u16(final byte[] b, final int at, final boolean little) {
        return little ? (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 : (b[at] & 0xFF) << 8 | (b[at + 1] & 0xFF);
    }

    private static long u32(final byte[] b, final int at, final boolean little) {
        return little ? (u16(b, at, true) & 0xFFFFL) | (long) u16(b, at + 2, true) << 16
                : (long) u16(b, at, false) << 16 | (u16(b, at + 2, false) & 0xFFFFL);
    }
}
