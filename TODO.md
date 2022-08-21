this is my own scratchpad for keeping track of things. it gets truncated frequently.

see CHANGELOG.md for a more formal list of changes by release

# done

* recommendations2, a local implementation of recommendations
    - done, sort of
        - still a bunch of finessing and correctness to do

# todo (0.0.2)

* api-gateway vs http-server projects
    - api-gateway would pose as a replacement for the api-gateway
        - access it the same way and it proxies requests back and forth
    - http-server would have namespaces and use replacement components
        - proxying requests only when a local implementation doesn't exist
    - this way we can compare the differences between: 
        - accessing the api-gateway directly
        - through the ppp proxy
            - which may also be configured to bypass the api-gateway and talk to the services directly
        - through the ppp reimplementation

* revisit running tests
    - I want to run a specific test
        - https://github.com/metabase/metabase/wiki/Migrating-from-Leiningen-to-tools.deps#running-specific-tests

* correctness testing
    - I want to compare calls to recommendations with local implementation

* speed testing
    - I want to compare speed of local recommendations vs remote

* stand-alone recommendations2 project
    - I want to build a standalone recommendations2 project
    - could it be a drop-in replacement for the current recommendations?

# consider

* rename 'content-type-list' in the component interfaces, to something else
    - this parameter is the list of acceptable content types and versions sent by the user
        - "acceptable-content-type-list" ?
    - it may be confusing with the other parameters
        - like "type" (content type) and "id" (content id) in recommendations

# todo bucket

* http-server, per-route caching rules
    - recommendations has some: 
        - https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/ApiResponse.php#L13
        - not sure if they're significant
* http, set 'vary' header
    - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Vary
    - https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/ApiResponse.php#L14
    - 'accept' header will vary response
* http, set response timeout
    - https://github.com/elifesciences/recommendations/blob/develop/src/bootstrap.php#L53
* http, set connection timeout
    - https://github.com/elifesciences/recommendations/blob/develop/src/bootstrap.php#L53
* http, validate keyword params
    - https://github.com/elifesciences/recommendations/blob/develop/src/bootstrap.php#L127
* http, caching
    - https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L299
* http, bunch of asynchronous calls at once
    - https://github.com/elifesciences/recommendations/blob/develop/src/bootstrap.php#L214
    - this looks like it may fail *all* requests if any *one* request fails?
* a 'bus' implementation for components to notify other components
* two implementations of the /articles interface
    - http proxy
        - done
    - local implementation
        - ...
* a journal renderer
    - like recommendations, it will be a greedy consumer
    - it will also be great to demonstrate *something*
    - it would be call to plug in the existing journal somehow, but not as a web service
* test speed of talking to (non-cdn) api gateway vs talking to services directly
    - with and without persistant connections
* component for each of the elife projects
* pass in configuration
    - from the environment?
        - not keen on pulling in environment vars
    - from a file?
        - preferably
* rebind configuration for certain operations
    - for example: I want to use lax directly and not via the api-gateway, so I need to rebind the 'host' and 'path' configuration for the 'lax' service
* command-line project has an 'uberjar' alias
    - isn't this centrally managed in root deps.edn?
* a gui
* a long list of use-cases
    - how to add a new api endpoint to system
    - how to handle error responses from api-gateway
    - where to put per-component dependencies
    - where to put shared dependencies
    - 
