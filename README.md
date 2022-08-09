# PPP, Pretty Perfect Publishing

An experiment in transitioning a microservices architecture to a monolith, preserving the good qualities and suppressing the bad.

This experiment uses Clojure with project organisation using Polylith:

---

<img src="logo.png" width="30%" alt="Polylith" id="logo">

The Polylith documentation can be found here:

- The [high-level documentation](https://polylith.gitbook.io/polylith)
- The [Polylith Tool documentation](https://github.com/polyfy/polylith)
- The [RealWorld example app documentation](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)

---

'ppp' was an internal name for the frontend infrastructure used by eLife before 'continuum'. 

It's also very short.

## usage

As a webserver:

    ./http-server

As a command line tool:

    ./cli <api-fn> [arg1 arg2 ... argN]

As a REPL:

    ./repl

As a REPL from within emacs + cider:

    c-x, c-f, deps.edn
    m-x, cider-jack-in

## testing

Run all tests:

    ./test

Run component tests:

    ./test <component>
