package org.dync.zxingscan;

import androidx.annotation.Nullable;

public final class Size {
    private final int zaa;
    private final int zab;

    public Size(int var1, int var2) {
        this.zaa = var1;
        this.zab = var2;
    }

    public final int getWidth() {
        return this.zaa;
    }

    public final int getHeight() {
        return this.zab;
    }

    public final boolean equals(@Nullable Object var1) {
        if (var1 == null) {
            return false;
        } else if (this == var1) {
            return true;
        } else if (var1 instanceof Size) {
            Size var2 = (Size)var1;
            return this.zaa == var2.zaa && this.zab == var2.zab;
        } else {
            return false;
        }
    }

    public final String toString() {
        int var1 = this.zaa;
        int var2 = this.zab;
        return (new StringBuilder(23)).append(var1).append("x").append(var2).toString();
    }

    private static NumberFormatException zaa(String var0) {
        throw new NumberFormatException((new StringBuilder(16 + String.valueOf(var0).length())).append("Invalid Size: \"").append(var0).append("\"").toString());
    }

    public static Size parseSize(String var0) throws NumberFormatException {
        if (var0 == null) {
            throw new IllegalArgumentException("string must not be null");
        } else {
            int var1;
            if ((var1 = var0.indexOf(42)) < 0) {
                var1 = var0.indexOf(120);
            }

            if (var1 < 0) {
                throw zaa(var0);
            } else {
                try {
                    return new Size(Integer.parseInt(var0.substring(0, var1)), Integer.parseInt(var0.substring(var1 + 1)));
                } catch (NumberFormatException var2) {
                    throw zaa(var0);
                }
            }
        }
    }

    public final int hashCode() {
        return this.zab ^ (this.zaa << 16 | this.zaa >>> 16);
    }
}

