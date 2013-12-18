(ns pennydreadful.test.handler
  (:use ring.mock.request
        pennydreadful.handler)
  (:require [expectations :refer :all]))

(expect 302
        (let [response (app (request :get "/"))]
          (:status response)))

