notes on drafting a release
* open a PR for master
    - check any new deps are present in licence
* merge on green
* co master
* tag release
* push tags
* co develop
* merge master
* truncate TODO
* update build.clj
    - version
* push
* create 'release' on github
    - paste changelog
