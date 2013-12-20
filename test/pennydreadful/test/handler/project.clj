(ns pennydreadful.test.handler.project
  (:require [expectations :refer :all]
            [ring.mock.request :refer :all]
            [pennydreadful.handler :refer [app]]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.test.handler.login :as login]))


(defn- make-post [body-content uri]
  (body (request :post uri) body-content))

(defn- post-new [project]
  (-> project
      (make-post "/project")
      (app)))

;; 200 for gets
(expect
 200
 (login/as-ryan
  (:status (app (request :get "/project")))))

(expect
 200
 (login/as-ryan
  (:status (app (request :get "/")))))

;; Post to project returns 201 Created
(expect
 201
 (login/as-ryan
  (:status (post-new {:name "deletrious debut"}))))

;; Post to project returns location of new project
(expect
 #"/project/.+"
 (login/as-ryan
  (let [response (post-new {:name "deletrious debut"})]
    (get-in response [:headers "Location"]))))


;; TODO test that returned URI is valid

;; Post to project actually adds project to database
(expect
 #{"accidental astronauts" "bromantic birdfeeders" "deletrious debut"}
 (login/as-ryan
  (post-new {:name "deletrious debut"})
  (let [ryan (data-user/user-for-username "ryan")
        projects (data-project/projects-for-user-eid (:id ryan))]
    (into #{} (map :name projects)))))
