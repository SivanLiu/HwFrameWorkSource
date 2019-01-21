package android.icu.text;

class CharsetRecog_UTF8 extends CharsetRecognizer {
    CharsetRecog_UTF8() {
    }

    String getName() {
        return "UTF-8";
    }

    CharsetMatch match(CharsetDetector det) {
        boolean hasBOM = false;
        int numValid = 0;
        int numInvalid = 0;
        byte[] input = det.fRawInput;
        int i = 0;
        if (det.fRawLength >= 3 && (input[0] & 255) == 239 && (input[1] & 255) == 187 && (input[2] & 255) == 191) {
            hasBOM = true;
        }
        while (true) {
            int i2 = i;
            if (i2 >= det.fRawLength) {
                break;
            }
            i = input[i2];
            if ((i & 128) != 0) {
                int trailBytes;
                if ((i & 224) == 192) {
                    trailBytes = 1;
                } else if ((i & 240) == 224) {
                    trailBytes = 2;
                } else if ((i & 248) == 240) {
                    trailBytes = 3;
                } else {
                    numInvalid++;
                }
                do {
                    i2++;
                    if (i2 >= det.fRawLength) {
                        break;
                    } else if ((input[i2] & 192) != 128) {
                        numInvalid++;
                        break;
                    } else {
                        trailBytes--;
                    }
                } while (trailBytes != 0);
                numValid++;
            }
            i = i2 + 1;
        }
        i = 0;
        if (hasBOM && numInvalid == 0) {
            i = 100;
        } else if (hasBOM && numValid > numInvalid * 10) {
            i = 80;
        } else if (numValid > 3 && numInvalid == 0) {
            i = 100;
        } else if (numValid > 0 && numInvalid == 0) {
            i = 80;
        } else if (numValid == 0 && numInvalid == 0) {
            i = 15;
        } else if (numValid > numInvalid * 10) {
            i = 25;
        }
        return i == 0 ? null : new CharsetMatch(det, this, i);
    }
}
