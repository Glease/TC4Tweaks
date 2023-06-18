# TC4Tweaks

A mod that adds a bit of functionality, a bit of performance and a bit of robustness into thaumcraft 4.

## Creating derivation

### License

See [COPYING](./COPYING).

### If you wish to upload to curseforge

If this repo is archived, or you cannot reach me via GitHub Issues, [this discord server](https://discord.gg/EdVX8Srm2c)
or email within 90 full days, you can safely assume I have give up maintaining this mod. 
If I'm no longer maintaining this mod, and you happen to find an opportunity to improve it, you can create a fork of this
mod and *upload it to curseforge under a different name*,
provided you still follow the license (AGPL 3.0 with forge exception) AND signing the jar with a key.

HOWEVER, please do remember to add the digest of your signing key to list of known keys
in [`./src/main/java/net/glease/tc4tweak/TC4Tweak.java`](src/main/java/net/glease/tc4tweak/TC4Tweak.java)

### If you just want to use it among your friends

As long as you comply with the AGPL license you are free to do whatever you want.

## Contributing

Generally, please follow the usual github workflow. Do not try to contact me in person as it might fail :P

### Reporting issue

Please use one of issue templates. Before you deleting everything in that template, think twice if it is indeed
inappropriate. That'd make everyone's life easier.

### Pull requests

I will merge your PR if it fits any one of these criteria:

1. Bug fix
2. Translation
3. Add a minor feature that does not require the other side to install/update this mod.
4. Add a major feature.
5. Any other useful change that somehow doesn't fall in any one of bugfix, translation or feature (does it really
   exist?).

Item 3 means, if your patch make TC4 only slightly better, e.g. speed up arcane soap using speed, it will not be merged
into the master. This is to ensure server owners and players can just grab and install a newer version of this mod
without having other people update it as well. Large servers takes weeks to move forward. This mod could however iterate
several versions within the same week. If people cannot pick new version then this new feature is useless. However, you
can keep the changes in a fork. This mod is AGPL anyway.

Monumental features falls in the category of item 4, thus bypassing the above restriction.

## Compat

### Thaumic Fixer

Not compatible. It's compiled using an ancient version of thaumcraft (yes ancient even by 1.7.10 standards).

### NotEnoughThaumcraftTabs

Not compatible. This mod supersedes that mod anyway.

### WitchingGadgets

Takes over the WG thaumonomicon index search functionality, to change where its stuff got drawn on higher browser scale.

### Minetweaker 3

Any kind of reload will invalidate any cache used

### Gregtech 6

My quick and dirty patch of `generateItemHash()` will disable itself when GT6 is detected.

### Other stuff

The mod is tested under the following two configurations:
1. GTNH latest dev pack
2. Only this mod and latest thaumcraft 4.

I used to install GT6 into my test env, but that slows down client down too much and is hampering my development speed.
Therefore, the compatibility with it would no longer comes as guarenteed.
However, I do wish to fix any future incompatibility if any more of these happens.

Other mod combination should also work but is not guaranteed.
Open a ticket in issues should you meet any problem.
