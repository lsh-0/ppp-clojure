
# done

* executable 'api-gateway'
    - java -jar ppp--api.jar 
        - behaves like the api-gateway for /articles
    - done

* executable 'repl'
    - loads a ns with 'api' available
        - (api/article-list)
        - (api/article 9560)
        - ...
    - utils also available
        - count articles?

* running tests
    - per-component
        - ./test.sh <component>
    - end2end
        - ./test.sh

* run the api-gateway, like the repl
    ./api-gateway
        - this is just 'running a project'
        - any project should be runnable
        - even the cli
            - but running the cli as a repl needs some extra shims
    - done
        - see ./web-server

* http-server should have api prefixed
    - https://localhost:8080/api/articles 
        - we can use nginx to rewrite api.gateway to gateway/api later
    - should allow 'http-server' to run not just the api gateway but all/any other routes at the same time
        - would we ever have a 'lax' project?
            - what would it do? standalone lax instance?
    - done

* flesh out the cli
    - repl should drop the user into the cli
        - done
    - cli should allow a function (or functions) to be executed and then exit
        - done
    - currently the repl pulls in ppp.lax.interface as 'api'
        - fixed
    - the cli should be doing something similar to ppp.http-server.core/api-routes
        - each of the interfaces is pulled in and made available
            - done
* http-proxy implementation of the lax component
    - done

# todo

* there is a 'cli' base that is skewing things
    - rename it 'command-line'

* two implementations of the /articles interface
    - http proxy
        - done
    - local implementation
        - ...

* another service added
    - so we can see where duplication in http proxying is happening
        - recommendations
            - because it's also greedy

* pass in configuration
    - from the environment?
        - not keen on pulling in environment vars
    - from a file?
        - preferably

* rebind configuration for certain operations
    - for example: I want to use lax directly and not via the api-gateway, so I need to rebind the 'host' and 'path' configuration for the 'lax' service

# todo bucket

* command-line project has an 'uberjar' alias
    - isn't this centrally managed in root deps.edn?
* a 'bus' implementation for components to notify other components
* test speed of talking to (non-cdn) api gateway vs talking to services directly
    - with and without persistant connections
* component for each of the elife projects
* a base (? a set of interfaces, right?) representing the api-raml
* a gui
* a long list of use-cases
