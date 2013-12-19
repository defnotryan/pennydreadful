(ns pennydreadful.test.data.user
  (:require [expectations :refer :all]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :refer :all]))

(expect
 {:username "ryan"}
 (in
  (with-populated-db
    (user-for-username "ryan"))))
