(ns pennydreadful.test.handler.login
  (:use ring.mock.request
        pennydreadful.handler)
  (:require [expectations :refer :all]
            [pennydreadful.test.util :as test-util]))

;; Accessing "/" unauthenticated bounces to login page
(expect
 302
 (test-util/with-populated-db
   (let [response (app (request :get "/"))]
     (:status response))))

(expect
 #"/login$"
 (test-util/with-populated-db
   (let [response (app (request :get "/"))]
     (-> response
         :headers
         (get "Location")))))


;; Able to log in with valid creds
(expect
 303
 (test-util/with-populated-db
   (let [login-request (body (request :post "/login") {:username "ryan" :password "Passw0rd!"})
         login-response (app login-request)]
     (:status login-response))))

(expect
 "/"
 (test-util/with-populated-db
   (let [login-request (body (request :post "/login") {:username "ryan" :password "Passw0rd!"})
         login-response (app login-request)]
     (-> login-response
         :headers
         (get "Location")))))

;; Not able to log in with invalid creds
(expect
 302
 (test-util/with-populated-db
   (let [login-request (body (request :post "/login") {:username "ryan" :password "WRONG!!!"})
         login-response (app login-request)]
     (:status login-response))))

(expect
 #"/login\?&login_failed=Y&username=ryan$"
 (test-util/with-populated-db
   (let [login-request (body (request :post "/login") {:username "ryan" :password "WRONG!!!"})
         login-response (app login-request)]
     (-> login-response
         :headers
         (get "Location")))))

;; Authenticates as ryan and creates a function that merges the session cookie into a provided request
(defn ryan-session [app-to-wrap]
  (let [login-request (body (request :post "/login") {:username "ryan" :password "Passw0rd!"})
        login-response (app-to-wrap login-request)
        session-cookie (-> login-response
                           :headers
                           (get "Set-Cookie")
                           (first))]
    (fn [req]
      (let [session-req (assoc-in req [:headers "cookie"] session-cookie)]
        (app-to-wrap session-req)))))

;; Executes ~@body with pennydreadful.handler/app wrapped in session authenticator
(defmacro as-ryan [& body]
  `(test-util/with-populated-db
     (with-redefs [app (ryan-session app)]
       ~@body)))
