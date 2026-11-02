package com.panda.commonground;

import java.util.List;

/** Every translation key the mod uses, so a test can check each has its English text. */
public final class Texts {
    /** The mod's name and description in mod lists that read them from the lang file. */
    public static final String NAME = "modmenu.nameTranslation.commonground";
    public static final String DESCRIPTION = "modmenu.descriptionTranslation.commonground";

    public static final List<String> ALL = List.of(NAME, DESCRIPTION);

    private Texts() {}
}
