#!/usr/bin/env lein-exec
;
; Copyright (C) 2007 Peter Monks
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
; 
;     http://www.apache.org/licenses/LICENSE-2.0
; 
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
; 
; This file is part of an unsupported extension to Alfresco.
; 

; This script creates a large number of folders with random filenames.  Each
; folder will contain "dirs-per-dir" directories, and there are five levels of
; nesting (so dirs-per-dir ^ 5 total directories are created).
;
; Dependencies:
; * lein-exec (https://github.com/kumarshantanu/lein-exec)
; 

(require '[clojure.java.io :as io])
(require '[clojure.string  :as s])

; Be very careful changing this, as it's raised to the power 5 to come up with the final directory list!
(def dirs-per-dir 12)

(def min-name-length 4)
(def max-name-length 20)

(def filename-chars
  [\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z
   \A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T \U \V \W \X \Y \Z
   \0 \1 \2 \3 \4 \5 \6 \7 \8 \9])

(def filename-chars-length (count filename-chars))

(defn random-filename
  "Return a random, valid directory name between min-name-length and max-name-length characters long."
  []
  (let [name-length (+ min-name-length (rand-int (- max-name-length min-name-length)))]
    (s/join
      (for [i (range name-length)]
           (nth filename-chars (rand-int filename-chars-length))))))

(def directories
  (for [a (for [i (range dirs-per-dir)] (random-filename))
        b (for [i (range dirs-per-dir)] (random-filename))
        c (for [i (range dirs-per-dir)] (random-filename))
        d (for [i (range dirs-per-dir)] (random-filename))
        e (for [i (range dirs-per-dir)] (random-filename))]
    (s/join "/" ["large-folder-structure" a b c d e "dummy.txt"])))

(println "Creating" (count directories) "random directories, 5 levels deep...")

(doall (map io/make-parents directories))

(println "Done")
