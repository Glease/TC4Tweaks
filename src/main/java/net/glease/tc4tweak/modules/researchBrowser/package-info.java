/**
 * ZLEVEL documentation:
 * It's not very obvious as how originally TC4 draws it GUI. However, this is how the part changed by TC4 is drawn
 *
 * 1. Border: -100.0. Border however will always be rendered with no regard to previous depth value.
 * 2. Navigation button: -50.0
 * 3. Everything else is assumed to be at 0.0
 */
package net.glease.tc4tweak.modules.researchBrowser;