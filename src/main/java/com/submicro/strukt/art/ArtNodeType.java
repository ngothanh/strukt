package com.submicro.strukt.art;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ArtNodeType {

    ART4(4, "ART4"),

    ART16(16, "ART16"),

    ART48(48, "ART48"),

    ART256(256, "ART256"),

    LEAF(0, "LEAF");

    private final int maxChildren;
    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }
}
