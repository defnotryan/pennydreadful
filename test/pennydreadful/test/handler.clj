(ns pennydreadful.test.handler
  (:use ring.mock.request
        pennydreadful.handler)
  (:require [expectations :refer :all]
            [pennydreadful.test.handler.login :refer :all]
            [pennydreadful.test.util :refer :all]))

;; Kick the tires
(expect
 200
 (as-ryan
  (let [response (app (request :get "/"))]
    (:status response))))

(expect
 200
 (as-ryan
  (let [response (app (request :get "/project"))]
    (:status response))))

(expect
 200
 (let [response (app (request :get "/js/site.js"))]
   (:status response)))

(expect
 404
 (as-ryan
  (let [response (app (request :get "/invalid"))]
    (:status response))))
