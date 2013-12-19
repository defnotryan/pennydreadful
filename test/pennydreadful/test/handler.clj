(ns pennydreadful.test.handler
  (:use ring.mock.request
        pennydreadful.handler)
  (:require [expectations :refer :all]
            [pennydreadful.test.handler.login :refer :all]
            [pennydreadful.test.util :refer :all]))

(expect
 200
 (as-ryan
  (let [response (app (request :get "/"))]
    (:status response))))
