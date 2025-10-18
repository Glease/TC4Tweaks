# TC4Tweaks

A mod that adds a bit of functionality, a bit of performance and a bit of robustness into thaumcraft 4.

## Maven

This project is hosted at https://maven.glease.net/repos/releases/. 
Snapshot releases are also available at https://maven.glease.net/repos/snapshots/.

NOTE: This maven does not have sufficient bandwidth for distribution of the mod to end user.
Please only use it as part of your development cycle or CI workflow. 

## API

The API of TC4Tweaks is available as a separate jar download both on maven and on mod distribution sites such as curseforge.
You are not expected to shade it under any condition. 
You are supposed to tell user to download and install the full TC4Tweaks jar.

If you only need the recipe lib, it's available as a separate library that you can shade.
Refer to its documentation for details.

## Github

Where I live have unstable internet connection, so I don't always immediately push new commits as releases are made.
You might see entries in changelog point to nonexistent commits, sorry for that.
If you need the source code, they are always available as additional files along with the main release jar and dev jar.
If you really need the commit history, please reach me in person via GitHub Issues, [this discord server](https://discord.gg/EdVX8Srm2c) or email.

## ASM conflict resolution

In general, I will automatically disable my stuff if it's known conflict with another mod currently installed.
If the other mod's asm/mixin is configurable, I'll only disable mine if the configuration is on.
Otherwise, my asm will be unconditionally applied.
Even if there is some sort of TC4Tweaks configuration that will advertise to disable part of the TC4Tweaks, the suggested ASM will still be applied when that part is disabled. 
This is because those configurations can all be modified without a game restart.

If you want more engaged compat with my ASMs, contact me via GitHub Issues, [this discord server](https://discord.gg/EdVX8Srm2c) 
or email.

## Creating derivation

### License

See [COPYING](./COPYING).

### If you wish to upload to curseforge

If this repo is archived, or you cannot reach me via any of the GitHub Issues, [this discord server](https://discord.gg/EdVX8Srm2c)
or email within 90 full days, you can safely assume I have give up maintaining this mod. 
If I'm no longer maintaining this mod, and you happen to find an opportunity to improve it, you can create a fork of this
mod and *upload it to curseforge under a different name*,
provided you still follow the license (AGPL 3.0 with forge exception) AND signing the jar with a key.

HOWEVER, please do remember to add the digest of your signing key to list of known keys
in [`./src/main/java/net/glease/tc4tweak/TC4Tweak.java`](src/main/java/net/glease/tc4tweak/TC4Tweak.java)

### If you just want to use it among your friends

As long as you comply with the AGPL license you are free to do whatever you want.

## Contributing

Generally, please follow the usual github workflow.

Like mentioned above, I only push to github occasionally (probably once a month, or less), so there is a very good chance code here is outdated.
If you have a contribution that require newer code, please contact me via GitHub Issues, [this discord server](https://discord.gg/EdVX8Srm2c) or email.
You could as well just send me the PR regardless (even if it contains my code from unpushed commits). 
I'll sort out the merging.

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

Not compatible. It has similar functionality and thus modify similar files, resulting a hard conflict.

### NotEnoughThaumcraftTabs

Not compatible. This mod supersedes that mod anyway.

### WitchingGadgets

Has explicit compatibility code.
Takes over the WG thaumonomicon index search functionality, to change where its stuff got drawn on higher browser scale.

### Minetweaker 3

Has explicit compatibility code.
Any kind of reload will invalidate any cache used by TC4Tweaks

### Gregtech 6

Has explicit compatibility code.
My quick and dirty patch of `generateItemHash()` will disable itself when GT6 is detected.

### Other stuff

The mod is tested under the following two configurations:
1. GTNH latest (up to 2.7.3 as of writing)
2. Only this mod and latest thaumcraft 4.

Additionally, this mod comes by default in GTNH, so you can assume it to be compatible anyway. 

I used to install GT6 into my test env, but that slows down client down too much and is hampering my development speed.
Therefore, the compatibility with it would no longer comes as guaranteed.
However, I do wish to fix any future incompatibility if any more of these happens.

Other mod combination should also work but is not guaranteed.
Open a ticket in issues should you meet any problem.
