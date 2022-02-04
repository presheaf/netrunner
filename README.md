Source code for the [Jinteki mirror](https://reteki.fun) used for the Reboot project. For card data and proxy generator tools, see my fork of the [netrunner-data](https://github.com/presheaf/netrunner-data/) repo.

Most of what I have done is patch together existing open source community tools written by other Netrunner community members. I'd like to put this a more discoverable place, but until I do, here is a list of projects I've used to run the Reteki server mentioned above, to whose creators I am grateful.

- [Jinteki.net](https://github.com/mtgred/netrunner/)
- [NetrunnerDB](https://github.com/NetrunnerDB/netrunnerdb)
- [Proxy Nexus](https://github.com/axmccx/proxynexus/tree/master/misc) - used their script to generate bleed borders on proxies, not to generate the proxies themselves
- [GRNDL Netrunner card creator](https://github.com/yonbergman/self-modifying-card) - used their proxy templates as starting point for proxy generator
- Stimslack user Skippan: assisted in cleanup and enhancement of proxy templates
- Probably others I am forgetting - please notify me if you know who!

All modifications I have made to assist in the setup of a Reteki mirror are open source in that they are available from my forks of the corresponding repositories, but I will be the first to admit they are frequently hacky and rarely easy to read. I am hopeful that I'll one day clean up my changes and make them easier to use for anyone wishing to set up their own Netrunner mirror with card changes, but because I doubt such usecases are very common, I don't feel too bad about postponing it for now. In case you *do* want to do this, please get in touch with me (Stimslack username: presheaf, or make an issue here), and I'll make a best effort to either get my act together or at least try to assist you in how to go about it. 
