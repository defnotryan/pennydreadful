(ns pennydreadful.test.handler.project
  (:require [expectations :refer :all]
            [ring.mock.request :refer :all]
            [clojure.edn :as edn]
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
 #"/project/[0-9]+"
 (login/as-ryan
  (let [response (post-new {:name "deletrious debut"})]
    (get-in response [:headers "Location"]))))


;; Post to project actually adds project to database
(expect
 #{"accidental astronauts" "bromantic birdfeeders" "deletrious debut"}
 (login/as-ryan
  (post-new {:name "deletrious debut"})
  (let [ryan (data-user/user-for-username "ryan")
        projects (data-project/projects-for-user-eid (:id ryan))]
    (into #{} (map :name projects)))))

;; Delete project
;; Ensure URI returned from POST is valid
(expect
 204
 (login/as-ryan
  (post-new {:name "Ke$ha: An autobiography" :description "I woke up in the morning feeling like P Diddy..."})
  (let [war-and-peace-response (post-new {:name "War and Peace" :description "A very meaningful novel."})
        war-and-peace (edn/read-string (:body war-and-peace-response))
        war-and-peace-eid (:id war-and-peace)
        delete-response (app (request :delete (str "/project/" war-and-peace-eid)))]
    (:status delete-response))))

(expect
 #{"accidental astronauts" "bromantic birdfeeders" "Ke$ha: An autobiography"}
 (login/as-ryan
  (post-new {:name "Ke$ha: An autobiography" :description "I woke up in the morning feeling like P Diddy..."})
  (let [war-and-peace-response (post-new {:name "War and Peace" :description "A very meaningful novel."})
        war-and-peace (edn/read-string (:body war-and-peace-response))
        war-and-peace-eid (:id war-and-peace)
        delete-response (app (request :delete (str "/project/" war-and-peace-eid)))]
    (let [ryan (data-user/user-for-username "ryan")
          projects (data-project/projects-for-user-eid (:id ryan))]
      (into #{} (map :name projects))))))

;; Cannot delete someone else's project
(expect
 {:status 401}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {war-eid :id} (data-project/insert-project! rhea-eid {:name "war and peace" :description "russian"})]
     (app (request :delete (str "/project/" war-eid)))))))

(expect
 {:name "war and peace" :description "russian"}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {war-eid :id} (data-project/insert-project! rhea-eid {:name "war and peace" :description "russian"})]
     (app (request :delete (str "/project/" war-eid)))
     (data-project/project-by-eid war-eid)))))

;; Put project changes database
(expect
 {:name "dauntless demographics" :description "no daunts here"}
 (in
  (login/as-ryan
   (let [dauntless-response (post-new {:name "dauntless demographics" :description "my new project"})
         dauntless (edn/read-string (:body dauntless-response))
         dauntless-eid (:id dauntless)]
     (-> (request :put (str "/project/" dauntless-eid))
         (body (assoc dauntless :description "no daunts here"))
         app)
     (data-project/project-by-eid dauntless-eid)))))

;; Put project response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [dauntless-response (post-new {:name "dauntless demographics" :description "my new project"})
         {dauntless-eid :id :as dauntless} (edn/read-string (:body dauntless-response))]
     (-> (request :put (str "/project/" dauntless-eid))
         (body (assoc dauntless :description "no daunts here"))
         app)))))

;; Cannot put to someone else's project
(expect
 {:name "dauntless demographics" :description "no daunts here"}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {dauntless-eid :id} (data-project/insert-project! rhea-eid {:name "dauntless demographics" :description "no daunts here"})]
     (-> (request :put (str "/project/" dauntless-eid))
         (body {:name "malicious" :description "malicious"})
         app)
     (data-project/project-by-eid dauntless-eid)))))

(expect
 {:status 401}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {dauntless-eid :id} (data-project/insert-project! rhea-eid {:name "dauntless demographics" :description "no daunts here"})]
     (-> (request :put (str "/project/" dauntless-eid))
         (body {:name "malicious" :description "malicious"})
         app)))))
