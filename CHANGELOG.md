# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.0.1

Initial release, the shape of an idea has been added.

### Added

* partial implementation of the api-gateway (lax, recommendations)
    - run with `./http-server` and then `curl http://localhost:8000/api/article-list`
* a repl that drops the user into `ppp.cli.core` by default with the `api` namespace available.
    - run with `./repl`
* a cli (command-line interface) that runs a single command from the api, prints the result, and exits.
    - run with `./cli <cmd> [arg1 arg2 ... argN]`
    - for example: 
        - `./cli article-list`
        - `./cli article 9560`

## [Unreleased]

### Added

### Changed

### Fixed

### Removed

