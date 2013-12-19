(ns pennydreadful.test.handler.login
  (:use ring.mock.request
        pennydreadful.handler)
  (:require [expectations :refer :all]
            [pennydreadful.test.util :as test-util]))

;; Authenticates as ryan and creates a function that merges the session cookie into a provided request
(defn ryan-session [app-to-wrap]
  (let [login-request (body (request :post "/login") {:username "ryan" :password "passw0rd!"})
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
   (let [login-request (body (request :post "/login") {:username "ryan" :password "passw0rd!"})
         login-response (app login-request)]
     (:status login-response))))

(expect
 "/"
 (test-util/with-populated-db
   (let [login-request (body (request :post "/login") {:username "ryan" :password "passw0rd!"})
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

;; Not able to access protected resources after logging out
(expect
 302
 (as-ryan
  (app (request :get "/logout"))
  (let [response (app (request :get "/project"))]
    (:status response))))

(expect
 #"/login$"
 (as-ryan
  (app (request :get "/logout"))
  (let [response (app (request :get "/project"))]
    (-> response
        :headers
        (get "Location")))))
