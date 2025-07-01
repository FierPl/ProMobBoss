package com.wattbreak.promobboss.integrations;

import lombok.Getter;

@Getter
public class SimpleMythicMob {
    private final String internalName;
    private final String displayName;

    public SimpleMythicMob(String internalName, String displayName) {
        this.internalName = internalName;
        this.displayName = displayName;
    }
}