package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

internal object SportsLottieSpecs {
    // 1. Live Broadcast Crimson Beacon Pulse (#FF4545, 64x64, 60 frames @ 30fps)
    const val LIVE_BEACON_JSON = """
{
  "v":"5.7.4","fr":30,"ip":0,"op":60,"w":64,"h":64,"nm":"live_beacon","ddd":0,"assets":[],
  "layers":[
    {
      "ddd":0,"ind":1,"ty":4,"nm":"ring","sr":1,
      "ks":{
        "o":{"a":1,"k":[{"t":0,"s":[75],"e":[0]},{"t":48,"s":[0]}]},
        "r":{"a":0,"k":0},
        "p":{"a":0,"k":[32,32,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":1,"k":[{"t":0,"s":[35,35,100],"e":[135,135,100]},{"t":48,"s":[135,135,100]}]}
      },
      "shapes":[
        {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[42,42]},"nm":"e1"},
        {"ty":"st","c":{"a":0,"k":[1,0.27,0.27,1]},"o":{"a":0,"k":100},"w":{"a":0,"k":4},"nm":"s1"}
      ],
      "ip":0,"op":60,"st":0
    },
    {
      "ddd":0,"ind":2,"ty":4,"nm":"core","sr":1,
      "ks":{
        "o":{"a":0,"k":100},
        "r":{"a":0,"k":0},
        "p":{"a":0,"k":[32,32,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":1,"k":[{"t":0,"s":[92,92,100],"e":[112,112,100]},{"t":30,"s":[112,112,100],"e":[92,92,100]},{"t":60,"s":[92,92,100]}]}
      },
      "shapes":[
        {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[18,18]},"nm":"e2"},
        {"ty":"fl","c":{"a":0,"k":[1,0.27,0.27,1]},"o":{"a":0,"k":100},"nm":"f2"}
      ],
      "ip":0,"op":60,"st":0
    }
  ]
}
"""

    // 2. Electric Lime Orbital Stadium Pulse for Splash & Refresh (120x120, 60 frames @ 30fps)
    const val STADIUM_ORBIT_JSON = """
{
  "v":"5.7.4","fr":30,"ip":0,"op":60,"w":120,"h":120,"nm":"stadium_orbit","ddd":0,"assets":[],
  "layers":[
    {
      "ddd":0,"ind":1,"ty":4,"nm":"outer_ring","sr":1,
      "ks":{
        "o":{"a":1,"k":[{"t":0,"s":[65],"e":[15]},{"t":55,"s":[15]}]},
        "r":{"a":1,"k":[{"t":0,"s":[0],"e":[360]},{"t":60,"s":[360]}]},
        "p":{"a":0,"k":[60,60,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":1,"k":[{"t":0,"s":[75,75,100],"e":[115,115,100]},{"t":55,"s":[115,115,100]}]}
      },
      "shapes":[
        {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[92,92]},"nm":"e_outer"},
        {"ty":"st","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":100},"w":{"a":0,"k":3},"nm":"s_outer"}
      ],
      "ip":0,"op":60,"st":0
    },
    {
      "ddd":0,"ind":2,"ty":4,"nm":"inner_hex","sr":1,
      "ks":{
        "o":{"a":0,"k":100},
        "r":{"a":1,"k":[{"t":0,"s":[0],"e":[180]},{"t":60,"s":[180]}]},
        "p":{"a":0,"k":[60,60,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":1,"k":[{"t":0,"s":[92,92,100],"e":[106,106,100]},{"t":30,"s":[106,106,100],"e":[92,92,100]},{"t":60,"s":[92,92,100]}]}
      },
      "shapes":[
        {"ty":"rc","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[44,44]},"r":{"a":0,"k":14},"nm":"r_core"},
        {"ty":"fl","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":20},"nm":"f_core"},
        {"ty":"st","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":100},"w":{"a":0,"k":3.5},"nm":"s_core"}
      ],
      "ip":0,"op":60,"st":0
    }
  ]
}
"""

    // 3. Minimal Empty/Signal Glow for Favorites & Empty Boards (96x96)
    const val EMPTY_CREST_JSON = """
{
  "v":"5.7.4","fr":30,"ip":0,"op":60,"w":96,"h":96,"nm":"empty_crest","ddd":0,"assets":[],
  "layers":[
    {
      "ddd":0,"ind":1,"ty":4,"nm":"halo","sr":1,
      "ks":{
        "o":{"a":1,"k":[{"t":0,"s":[45],"e":[12]},{"t":30,"s":[12],"e":[45]},{"t":60,"s":[45]}]},
        "r":{"a":0,"k":0},
        "p":{"a":0,"k":[48,48,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":1,"k":[{"t":0,"s":[88,88,100],"e":[108,108,100]},{"t":30,"s":[108,108,100],"e":[88,88,100]},{"t":60,"s":[88,88,100]}]}
      },
      "shapes":[
        {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[68,68]},"nm":"e_halo"},
        {"ty":"st","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":100},"w":{"a":0,"k":2.5},"nm":"s_halo"}
      ],
      "ip":0,"op":60,"st":0
    },
    {
      "ddd":0,"ind":2,"ty":4,"nm":"center_dot","sr":1,
      "ks":{
        "o":{"a":0,"k":100},
        "r":{"a":0,"k":0},
        "p":{"a":0,"k":[48,48,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":0,"k":[100,100,100]}
      },
      "shapes":[
        {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[22,22]},"nm":"e_dot"},
        {"ty":"fl","c":{"a":0,"k":[0.22,0.74,0.97,1]},"o":{"a":0,"k":32},"nm":"f_dot"},
        {"ty":"st","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":100},"w":{"a":0,"k":2.5},"nm":"s_dot"}
      ],
      "ip":0,"op":60,"st":0
    }
  ]
}
"""

    // 4. Electric Lime Goal Event Celebration Burst (96x96, 48 frames @ 30fps)
    const val GOAL_EVENT_JSON = """
{
  "v":"5.7.4","fr":30,"ip":0,"op":48,"w":96,"h":96,"nm":"goal_burst","ddd":0,"assets":[],
  "layers":[
    {
      "ddd":0,"ind":1,"ty":4,"nm":"shockwave","sr":1,
      "ks":{
        "o":{"a":1,"k":[{"t":0,"s":[90],"e":[0]},{"t":40,"s":[0]}]},
        "r":{"a":0,"k":0},
        "p":{"a":0,"k":[48,48,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":1,"k":[{"t":0,"s":[35,35,100],"e":[138,138,100]},{"t":40,"s":[138,138,100]}]}
      },
      "shapes":[
        {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[62,62]},"nm":"e_wave"},
        {"ty":"st","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":100},"w":{"a":0,"k":4},"nm":"s_wave"}
      ],
      "ip":0,"op":48,"st":0
    },
    {
      "ddd":0,"ind":2,"ty":4,"nm":"crest_pop","sr":1,
      "ks":{
        "o":{"a":0,"k":100},
        "r":{"a":1,"k":[{"t":0,"s":[-12],"e":[12]},{"t":24,"s":[12],"e":[-12]},{"t":48,"s":[-12]}]},
        "p":{"a":0,"k":[48,48,0]},
        "a":{"a":0,"k":[0,0,0]},
        "s":{"a":1,"k":[{"t":0,"s":[86,86,100],"e":[112,112,100]},{"t":24,"s":[112,112,100],"e":[86,86,100]},{"t":48,"s":[86,86,100]}]}
      },
      "shapes":[
        {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[32,32]},"nm":"e_ball"},
        {"ty":"fl","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":28},"nm":"f_ball"},
        {"ty":"st","c":{"a":0,"k":[0.78,1,0.24,1]},"o":{"a":0,"k":100},"w":{"a":0,"k":3.5},"nm":"s_ball"}
      ],
      "ip":0,"op":48,"st":0
    }
  ]
}
"""
}

@Composable
fun LiveBeaconLottie(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(SportsLottieSpecs.LIVE_BEACON_JSON)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = !reducedMotion,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { if (reducedMotion) 0.25f else progress },
        modifier = modifier.size(size)
    )
}

@Composable
fun LoadingPitchLottie(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(SportsLottieSpecs.STADIUM_ORBIT_JSON)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = !reducedMotion,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { if (reducedMotion) 0.35f else progress },
        modifier = modifier.size(size)
    )
}

@Composable
fun EmptyCrestLottie(
    modifier: Modifier = Modifier,
    size: Dp = 74.dp
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(SportsLottieSpecs.EMPTY_CREST_JSON)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = !reducedMotion,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { if (reducedMotion) 0.5f else progress },
        modifier = modifier.size(size)
    )
}

/**
 * Short celebration Lottie burst rendered beside goal events or live goal alerts.
 */
@Composable
fun GoalEventAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    iterations: Int = 3
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(SportsLottieSpecs.GOAL_EVENT_JSON)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = !reducedMotion,
        iterations = iterations
    )
    LottieAnimation(
        composition = composition,
        progress = { if (reducedMotion) 0.45f else progress },
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "انیمیشن ثبت گل" }
            .testTag("goal_event_animation")
    )
}
