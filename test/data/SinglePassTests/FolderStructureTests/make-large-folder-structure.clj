#!/usr/bin/env lein-exec
(require '[clojure.java.io :as io])
(require '[clojure.string  :as s])

; Be very careful changing this, as it's raised to the power 5 to come up with the final directory list
(def dir-count 13)

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
  (for [a (for [i (range dir-count)] (random-filename))
        b (for [i (range dir-count)] (random-filename))
        c (for [i (range dir-count)] (random-filename))
        d (for [i (range dir-count)] (random-filename))
        e (for [i (range dir-count)] (random-filename))]
    (s/join "/" ["large-folder-structure" a b c d e "dummy.txt"])))

(println "Creating" (count directories) "directories...")

(doall (map io/make-parents directories))
