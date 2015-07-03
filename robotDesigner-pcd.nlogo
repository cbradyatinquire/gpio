extensions [ gpio ]

globals [ runtime-context background-color line-color wall-color  mouse-was-down? file-counter html-filename html-file-counter grabbed-robot grab-counter
  right-forward-pin 
  right-back-pin 
  left-forward-pin 
  left-back-pin 
]

breed [ robots robot ]
breed [ blocks block ]
breed [ lights light ]

patches-own [ lumens unlit-color colorable]

to setup-robots
  ca
  set background-color 3
  set wall-color red
  set line-color black 
  set mouse-was-down? false
  set runtime-context "screen"
  
  setup-patches
  set-default-shape robots "robot"
  create-the-robots
  
  setup-gpio-pins
  
  reset-ticks
end

to setup-patches
  ask patches [ set unlit-color background-color set pcolor background-color set colorable true]
end


to create-the-robots
  create-robots howmany-robots
  [
    move-to one-of patches  
    set size 3
    set color lime - 1
  ]
end

to run-for-screen-robots
  every delay [
    set runtime-context "screen"
    ask robots [
      let normal true
      
      if is-right-brighter-than-left [ set normal false  run right-brighter-than-left ]
      if is-left-brighter-than-right [ set normal false  run left-brighter-than-right ]
      
      if is-right-color-darker-than-left [ set normal false  run right-color-darker-than-left ]
      if is-left-color-darker-than-right [ set normal false  run left-color-darker-than-right ]
      
      if is-contact-with-left-bump-sensor  [ set normal false  run contact-with-left-bump-sensor ]
      if is-contact-with-right-bump-sensor [ set normal false  run contact-with-right-bump-sensor ]
      
      if (normal) [ run when-there-is-no-event ]
    ]
  ]
end


to run-for-physical-robot
  setup-gpio-pins
  set runtime-context "physical"
  let normal true
  
  if gpio:digital-read color-sensor-LEFT  > gpio:digital-read color-sensor-RIGHT [ set normal false  run right-color-darker-than-left ]
  if gpio:digital-read color-sensor-RIGHT >  gpio:digital-read color-sensor-LEFT [ set normal false  run left-color-darker-than-right ]
  
  
  if gpio:digital-read bump-sensor-LEFT  = 0 [ set normal false  run contact-with-left-bump-sensor ]
  if gpio:digital-read bump-sensor-RIGHT = 0 [ set normal false  run contact-with-right-bump-sensor ]
  
  ;;OUR ROBOTS DON'T HAVE A LIGHT SENSOR
  if (normal) [ run when-there-is-no-event ]
end

to setup-gpio-pins
  set right-forward-pin 8
  set right-back-pin 11
  set left-forward-pin 12
  set left-back-pin 13
  
  gpio:set-mode color-sensor-RIGHT "READ"
  gpio:set-mode color-sensor-LEFT "READ"
  gpio:set-mode bump-sensor-RIGHT "READ"
  gpio:set-mode bump-sensor-LEFT "READ"
  
  gpio:set-mode right-forward-pin "WRITE"
  gpio:set-mode right-back-pin "WRITE"
  gpio:set-mode left-forward-pin "WRITE"
  gpio:set-mode left-back-pin "WRITE"
  
  carefully [
  let feedback gpio:pwm-set-level 9  80
  set feedback gpio:pwm-set-level 10 80
  ]
  [ show error-message ]
end


to move-a-robot
  ifelse mouse-was-down?
  [
    ifelse mouse-down?
    [
       if grabbed-robot != nobody
       [
         set grab-counter grab-counter + 1
         ask grabbed-robot [
            setxy mouse-xcor mouse-ycor
            if grab-counter mod 100 = 0 [ set heading heading + 1 ] 
         ] 
       ] 
    ]
    [
      set mouse-was-down? false
      stop
    ]
  ]
  [
    if (mouse-down? and any? robots with [ distancexy mouse-xcor mouse-ycor < 3 ])
    [
      set mouse-was-down? true
      set grabbed-robot one-of robots with [ distancexy mouse-xcor mouse-ycor < 3 ]
    ]
  ]
end


;;;CREATE THE ENVIRONMENT
;;;Drawing and erasing lines
to draw-lines
  ifelse (mouse-down?)
  [
   set mouse-was-down? true
   ask patch mouse-xcor mouse-ycor  [
     if unlit-color = background-color [ set unlit-color line-color set pcolor line-color set colorable false  ] 
   ]
  ]
  [
    if ( mouse-was-down?)
    [
     set mouse-was-down? false
     stop 
    ]
  ]
end

to erase-lines
  ifelse (mouse-down?)
  [
   set mouse-was-down? true
   ask patch mouse-xcor mouse-ycor  [
     if unlit-color = line-color [ set unlit-color background-color set colorable true color-by-lumens] 
   ]
  ]
  [
    if ( mouse-was-down?)
    [
     set mouse-was-down? false
     stop 
    ]
  ]
end


;;;Drawing and erasing walls
to draw-walls
  ifelse (mouse-down?)
  [
   set mouse-was-down? true
   let new-wall-patches (patch-set patch mouse-xcor mouse-ycor   patch (mouse-xcor - 1) mouse-ycor    patch (mouse-xcor + 1) mouse-ycor  patch mouse-xcor (mouse-ycor - 1)    patch mouse-xcor ( mouse-ycor + 1)) 
   ask new-wall-patches [
     if unlit-color = background-color [ set unlit-color wall-color set pcolor wall-color set colorable false] 
   ]
  ]
  [
    if ( mouse-was-down?)
    [
     set mouse-was-down? false
     stop 
    ]
  ]
end

to erase-walls
  ifelse (mouse-down?)
  [
   set mouse-was-down? true
   let patches-to-erase (patch-set patch mouse-xcor mouse-ycor   patch (mouse-xcor - 1) mouse-ycor    patch (mouse-xcor + 1) mouse-ycor  patch mouse-xcor (mouse-ycor - 1)    patch mouse-xcor ( mouse-ycor + 1)) 
   
   ask patches-to-erase  [
     if unlit-color = wall-color [ set unlit-color background-color set colorable true color-by-lumens ] 
   ]
  ]
  [
    if ( mouse-was-down?)
    [
     set mouse-was-down? false
     stop 
    ]
  ]
end

;;Placing and removing lights
to place-lights
  if (mouse-down? and not any? lights with [ distancexy mouse-xcor mouse-ycor < 3 ])
  [
    create-lights 1 [ 
      setxy mouse-xcor mouse-ycor
      set size 2.5
      set shape "lightbulb" 
      set heading 0
      set color orange + 1
    ]
    ask patches [ calculate-lumens ]
    ask patches [ color-by-lumens ]
    set mouse-was-down? false
    stop
  ]
end

to remove-lights
  if (mouse-down?)
  [
    let to-remove lights with [ distancexy mouse-xcor mouse-ycor < 3 ] 
    if any? to-remove [
      ask to-remove [die]
      ask patches [ calculate-lumens ]
      ask patches [ color-by-lumens ]
      set mouse-was-down? false
      stop
    ]
  ]
end


;;;AMBIENT LIGHT CALCULATIONS
to calculate-lumens
  let my-lumens 0
  ask lights [ set my-lumens my-lumens + 20 / ((distance  myself) * (distance  myself)) ]
  set lumens my-lumens
end

to color-by-lumens
  if (colorable) [
    ifelse lumens > 0 [set pcolor scale-color yellow lumens -.02 .45]
    [ set pcolor unlit-color ]
  ]
end

;;SENSOR LOGIC
to-report is-right-brighter-than-left
  report ([lumens] of patch-right-and-ahead 90 1) - ([lumens] of patch-left-and-ahead 90 1) > .02
end

to-report is-left-brighter-than-right
  report ([lumens] of patch-left-and-ahead 90 1) - ([lumens] of patch-right-and-ahead 90 1) > .02
end

to-report is-right-color-darker-than-left
  report ([unlit-color] of patch-right-and-ahead 55 1 = line-color)  and ([unlit-color] of patch-left-and-ahead 55 1 != line-color)
end

to-report is-left-color-darker-than-right
  report ([unlit-color] of patch-left-and-ahead 55 1 = line-color)  and ([unlit-color] of patch-right-and-ahead 55 1 != line-color)
end

to-report is-contact-with-left-bump-sensor
  report [unlit-color] of patch-left-and-ahead 45 1 = wall-color
end

to-report is-contact-with-right-bump-sensor
  report [unlit-color] of patch-right-and-ahead 45 1 = wall-color
end


;;;WHEEL MOVEMENT CONTROLS.
to right-wheel-forward [ speed ]
  ifelse (runtime-context = "screen")
  [
    repeat speed [
      forward .01
      right .1 
      display
    ]
  ]
  [
    gpio:digital-write right-back-pin "LOW"
    gpio:digital-write right-forward-pin "HIGH"
    wait speed / 3
    gpio:digital-write right-forward-pin "LOW"
  ]
end

to right-wheel-backward [ speed ]
  ifelse (runtime-context = "screen")
  [
    repeat speed [
      bk .01
      left .1 
      display
    ]
  ]
  [
    gpio:digital-write right-forward-pin "LOW"
    gpio:digital-write right-back-pin  "HIGH"
    wait speed / 3
    gpio:digital-write right-back-pin "LOW"
  ]
end

to left-wheel-forward [ speed ]
  ifelse (runtime-context = "screen")
  [
    repeat speed [
      forward .01
      left .1 
      display
    ]
  ]
  [
    gpio:digital-write left-back-pin "LOW"
    gpio:digital-write left-forward-pin "HIGH"
    wait speed / 3
    gpio:digital-write left-forward-pin "LOW"
  ]
end

to left-wheel-backward [ speed ]
  ifelse (runtime-context = "screen")
  [
    repeat speed [
      back .01
      right .1
      display 
    ]
  ]
  [
    gpio:digital-write left-forward-pin "LOW"
    gpio:digital-write left-back-pin  "HIGH"
    wait speed / 3
    gpio:digital-write left-back-pin "LOW"
  ]
  
end

to write-html-header
  file-print "<HTML><BODY>\n"  
end


;;FILE OUTPUT
to take-a-photo
  let caption user-input "Input a caption for this picture if you wish.\n(Leave blank for no caption; HALT to cancel taking a picture):"
  
  let filename (word "screen" file-counter ".jpg")
  while [file-exists? filename] [ 
    set file-counter file-counter + 1
    set filename (word "screen" file-counter ".jpg")
  ]
  export-interface filename
  
  let write-header false
  if (html-filename = 0) [
    set write-header true
    set html-filename (word "RobotScrapbook" html-file-counter ".html") 
    while [ file-exists? html-filename ] [
      set html-file-counter html-file-counter + 1
      set html-filename (word "RobotScrapbook" html-file-counter ".html")
    ]
  ]
  file-open html-filename
  if (write-header) [ write-html-header ]
  file-print (word "Interface capture #" file-counter "<br>Saved at " date-and-time ":<br><img src='" filename "'></img><br>\n")
  if (caption != "" and caption != 0 and caption != false) [ file-print (word "Caption: <b>" caption "</b><br><br>") ]
  file-close-all
end


to save-world-state
  let fname user-new-file 
  if fname != false
  [
   export-world fname 
  ]
end

to load-world-state
  let fname user-file 
  if fname != false
  [
   let fc file-counter
   let hfc html-file-counter
   let hfn html-filename
   import-world fname 
   set file-counter fc
   set html-file-counter hfc
   set html-filename hfn
  ]
end
@#$#@#$#@
GRAPHICS-WINDOW
440
10
931
522
25
25
9.45
1
10
1
1
1
0
1
1
1
-25
25
-25
25
0
0
1
ticks
30.0

INPUTBOX
10
35
215
140
right-brighter-than-left
right-wheel-forward 2\n;right-wheel-backward 1\n;left-wheel-forward 1\n;left-wheel-backward 1
1
0
String (commands)

INPUTBOX
220
35
435
140
left-brighter-than-right
;right-wheel-forward 1\n;right-wheel-backward 1\nleft-wheel-forward 2\n;left-wheel-backward 1
1
0
String (commands)

INPUTBOX
10
145
215
250
right-color-darker-than-left
right-wheel-forward 2\n;right-wheel-backward 1\n;left-wheel-forward 1\nleft-wheel-backward 2
1
0
String (commands)

INPUTBOX
220
145
435
250
left-color-darker-than-right
;right-wheel-forward 1\nright-wheel-backward 2\nleft-wheel-forward 2\n;left-wheel-backward 1
1
0
String (commands)

INPUTBOX
10
255
215
380
contact-with-right-bump-sensor
;right-wheel-forward 1\nright-wheel-backward 20\nleft-wheel-forward 10\nleft-wheel-backward 15 
1
0
String (commands)

INPUTBOX
220
255
435
380
contact-with-left-bump-sensor
;right-wheel-forward 1\nleft-wheel-backward 20\nright-wheel-forward 10\nright-wheel-backward 15 
1
0
String (commands)

BUTTON
60
485
215
545
NIL
run-for-screen-robots
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
0

BUTTON
10
435
75
468
setup
setup-robots
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
80
435
215
468
howmany-robots
howmany-robots
1
50
6
1
1
NIL
HORIZONTAL

BUTTON
940
60
1060
93
Draw a Line
draw-lines
T
1
T
OBSERVER
NIL
L
NIL
NIL
1

BUTTON
940
150
1060
183
Draw a Wall
draw-walls
T
1
T
OBSERVER
NIL
W
NIL
NIL
1

BUTTON
940
95
1060
128
Erase a Line
erase-lines
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
940
185
1060
218
Erase a Wall
erase-walls
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
940
240
1060
273
Place a Light
place-lights
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
940
275
1060
308
Remove a Light
remove-lights
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
10
550
215
583
delay
delay
0
.005
0
.0001
1
sec
HORIZONTAL

TEXTBOX
940
10
1075
50
DESIGN VIRTUAL ENVIRONMENT
16
123.0
1

TEXTBOX
15
10
425
28
PROGRAM YOUR SCREEN & PHYSICAL ROBOTS
16
123.0
1

TEXTBOX
10
480
60
540
RUN YOUR CODE
16
123.0
1

TEXTBOX
10
410
205
428
SETUP SCREEN ROBOTS
16
123.0
1

BUTTON
945
440
1065
485
Take a Photo
take-a-photo
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
945
495
1065
530
Save World State
save-world-state
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
945
535
1065
570
Load a World State
load-world-state
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
940
335
1060
368
Move a Robot
move-a-robot
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
280
525
435
585
NIL
run-for-physical-robot
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

INPUTBOX
570
525
680
585
color-sensor-LEFT
6
1
0
Number

INPUTBOX
685
525
795
585
color-sensor-RIGHT
7
1
0
Number

INPUTBOX
440
525
555
585
bump-sensor-LEFT
2
1
0
Number

INPUTBOX
810
525
925
585
bump-sensor-RIGHT
3
1
0
Number

TEXTBOX
945
395
1040
431
CAPTURE / RECORD
16
123.0
1

INPUTBOX
220
385
435
520
when-there-is-no-event
right-wheel-forward 1\nleft-wheel-forward 1\n;\n;
1
0
String (commands)

@#$#@#$#@
## WHAT IS IT?

(a general understanding of what the model is trying to show or explain)

## HOW IT WORKS

(what rules the agents use to create the overall behavior of the model)

## HOW TO USE IT

(how to use the model, including a description of each of the items in the Interface tab)

## THINGS TO NOTICE

(suggested things for the user to notice while running the model)

## THINGS TO TRY

(suggested things for the user to try to do (move sliders, switches, etc.) with the model)

## EXTENDING THE MODEL

(suggested things to add or change in the Code tab to make the model more complicated, detailed, accurate, etc.)

## NETLOGO FEATURES

(interesting or unusual features of NetLogo that the model uses, particularly in the Code tab; or where workarounds were needed for missing features)

## RELATED MODELS

(models in the NetLogo Models Library and elsewhere which are of related interest)

## CREDITS AND REFERENCES

(a reference to the model's URL on the web if it has one, as well as any other necessary credits, citations, and links)
@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

lightbulb
false
8
Polygon -11221820 true true 219 104 205 133 195 165 174 190 165 210 165 225 150 225 147 119
Polygon -11221820 true true 79 103 95 133 105 165 126 190 135 210 135 225 150 225 154 120
Rectangle -7500403 true false 105 165 195 273
Line -16777216 false 135 180 135 255
Line -16777216 false 165 180 165 255
Line -16777216 false 150 180 150 255
Circle -11221820 true true 73 0 152
Line -16777216 false 180 180 180 255
Line -16777216 false 120 180 120 255

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

robot
true
0
Polygon -7500403 true true 151 8 119 10 98 25 86 48 82 225 83 271 105 289 150 294 195 291 218 272 217 225 214 47 201 24 181 11
Polygon -16777216 true false 210 195 195 210 195 135 210 105
Polygon -16777216 true false 105 255 120 270 180 270 195 255 195 225 105 225
Polygon -16777216 true false 90 195 105 210 105 135 90 105
Polygon -1 true false 205 29 180 30 181 11
Line -7500403 true 210 165 195 165
Line -7500403 true 90 165 105 165
Polygon -16777216 true false 121 135 180 134 204 97 182 89 153 85 120 89 98 97
Line -16777216 false 210 90 195 30
Line -16777216 false 90 90 105 30
Polygon -1 true false 95 29 120 30 119 11
Rectangle -13840069 true false 45 150 75 255
Rectangle -13840069 true false 225 150 255 255

sheep
false
15
Circle -1 true true 203 65 88
Circle -1 true true 70 65 162
Circle -1 true true 150 105 120
Polygon -7500403 true false 218 120 240 165 255 165 278 120
Circle -7500403 true false 214 72 67
Rectangle -1 true true 164 223 179 298
Polygon -1 true true 45 285 30 285 30 240 15 195 45 210
Circle -1 true true 3 83 150
Rectangle -1 true true 65 221 80 296
Polygon -1 true true 195 285 210 285 210 240 240 210 195 210
Polygon -7500403 true false 276 85 285 105 302 99 294 83
Polygon -7500403 true false 219 85 210 105 193 99 201 83

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

wolf
false
0
Polygon -16777216 true false 253 133 245 131 245 133
Polygon -7500403 true true 2 194 13 197 30 191 38 193 38 205 20 226 20 257 27 265 38 266 40 260 31 253 31 230 60 206 68 198 75 209 66 228 65 243 82 261 84 268 100 267 103 261 77 239 79 231 100 207 98 196 119 201 143 202 160 195 166 210 172 213 173 238 167 251 160 248 154 265 169 264 178 247 186 240 198 260 200 271 217 271 219 262 207 258 195 230 192 198 210 184 227 164 242 144 259 145 284 151 277 141 293 140 299 134 297 127 273 119 270 105
Polygon -7500403 true true -1 195 14 180 36 166 40 153 53 140 82 131 134 133 159 126 188 115 227 108 236 102 238 98 268 86 269 92 281 87 269 103 269 113

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270

@#$#@#$#@
NetLogo 5.0
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
default
0.0
-0.2 0 0.0 1.0
0.0 1 1.0 0.0
0.2 0 0.0 1.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180

@#$#@#$#@
1
@#$#@#$#@
