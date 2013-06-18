(ns la-vie-en-cloj.bounce
  (:require [mondrian.anim :as anim]
            [mondrian.canvas :as canvas]
            [mondrian.color :as color]
            [mondrian.math :as math]
            [mondrian.plot :as plot]
            [mondrian.ui :as ui]
            [monet.canvas :as m])
  (:use-macros [dommy.macros :only [sel1]]))

;; ---------------------------------------------------------------------
;; Update pipeline
;;
;; Initial state:
;;    :drawing -- The top-level DOM element that contains the canvas,
;;      the sliders, etc.
;;    :ctx -- The canvas context.
;;    :w -- The width of the canvas.
;;    :h -- The height of the canvas.
;;
;; Configuration values (as defined on the mondrian element):
;;    :ball-size -- Radius of the circle in pixels.
;;    :speed-pps -- The speed of the circle in pixels-per-second.
;;    :persist-image -- Whether or not to clear the background in
;;      between frames.
;;
;; After merge-control-values:
;;    :... -- One key for each control (identified by the control name).
;;      Zero or more controls may be present under the mondrian element,
;;      in which case the missing values must have been provided in as
;;      defaults in the element itself.
;;
;; After move:
;;    :x -- X location of the center of the circle (may be off-canvas
;;      depending on the speed and the framerate).
;;    :y -- Y location of the center of the circle (may be off-canvas
;;      depending on the speed and the framerate).
;;    :direction -- Direction (in radians) in which the circle is moving.
;;
;; After bounce:
;;    :x -- X location of the center of the circle after bouncing the
;;      circle off of walls (if necessary).
;;    :x -- Y location of the center of the circle after bouncing the
;;      circle off of walls (if necessary).

(defn merge-control-values
  "Merge the current values of the controls into state."
  [{:keys [drawing] :as state}]
  (merge state (ui/update-controls drawing)))

(defn move
  [{:keys [delta-t-ms speed-pps x y direction] :as state}]
  (let [direction (math/radians direction)
        pixels-per-millisecond (/ speed-pps 1000)
        delta-pixels (* delta-t-ms pixels-per-millisecond)
        dx (math/circle-x delta-pixels direction)
        dy (math/circle-y delta-pixels direction)]
    (assoc state :x (+ x dx) :y (+ y dy))))

(defn bounce
  [{:keys [w h x y] :as state}]
  (cond
    (not (< 0 x w)) (update-in state [:direction] #(- 180 %))
    (not (< 0 y h)) (update-in state [:direction] #(- 360 %))
    :else state))

(defn update-pipeline
  [state]
  (-> state
      merge-control-values
      move
      bounce))


;; ---------------------------------------------------------------------
;; Render stack
;;

(defn clear-background
  [{:keys [ctx w h persist-image]}]
  (when-not persist-image
    (-> ctx
        (m/fill-style "rgba(25,29,33,0.75)") ;; Alpha adds motion blur
        (m/fill-rect {:x 0 :y 0 :w w :h h}))))

(defn draw-ball
  [{:keys [ctx ball-size x y]}]
  (m/fill-style ctx "red")
  (m/circle ctx {:x x :y y :r ball-size}))

(defn render-stack
  [state]
  (clear-background state)
  (draw-ball state))


;; ---------------------------------------------------------------------
;; Main entry point
;;

(defn ^:export start
  "Starts the bouncing ball animation given the top-level DOM element that
  contains the canvas and any controls."
  [drawing]
  (let [canvas (sel1 drawing :canvas)
        ctx (m/get-context canvas "2d")
        [w h] (ui/setup-canvas canvas ctx 1.0)
        fixed-state {:drawing drawing :ctx ctx :w w :h h}
        init-state {:x 100 :y 100 :direction (rand-int 360)}
        state (merge fixed-state init-state)]
    (anim/start state
                #(update-pipeline %)
                #(render-stack %)
                #(-> ctx
                     (m/fill-style "red")
                     (m/font-style "sans-serif")
                     (m/text {:text % :x 0 :y 20})))))
