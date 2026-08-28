# Clock Builder

The builder creates comparator clocks and classic Ethonian hopper clocks at the player's feet, aligned to the horizontal view direction.

```mcfunction
/clock <interval>
/clock comparator <interval>
/clock hopper <interval>
/clock undo
```

`comparator` is the default type. Intervals may be plain Redstone ticks (`2`), explicit Redstone ticks (`2t`), or seconds (`1s`). One second equals 10 Redstone ticks. Unsupported values return an error rather than being rounded.

## Comparator clocks

Comparator clocks support even periods from 2 to 600 Redstone ticks by default. The interval is the complete period from one rising pulse to the next, including on and off phases.

The first four possible periods are:

| Redstone ticks | Seconds | Example |
| ---: | ---: | --- |
| 2 | 0.2 | `/clock 2t` |
| 4 | 0.4 | `/clock 4t` |
| 6 | 0.6 | `/clock 6t` |
| 8 | 0.8 | `/clock 8t` |

The feedback loop uses the smallest supported two-row layout. A two-tick clock contains the subtract-mode comparator and dust:

```text
#x
xx
```

A four-tick clock adds one repeater:

```text
#xx
x<-x
```

`#` is the subtract-mode comparator, arrows are repeaters, and `x` is Redstone dust. A Redstone block behind the comparator provides input. Longer periods use the fewest repeaters, each configured for up to four ticks. The final repeater regenerates signal strength before the subtraction input.

## Hopper clocks

The classic flat 2-by-6 Ethonian layout is:

```text
B <C H> <H C> B
x P> R  .  <P x
```

`H` marks inward-facing hoppers, `C` outward-facing comparators, `P` sticky pistons, `R` the initial Redstone-block position, `.` its alternate position, `x` dust, and `B` solid blocks.

The exact period is `8 × items − 6` Redstone ticks for two or more items. One item is a 7-tick special case; two items produce 10 ticks, and each additional item adds 8 ticks. Valid periods are 7 ticks or `10 + 8n` ticks up to 2,554 ticks with the default server limit. Examples include `/clock hopper 7t`, `/clock hopper 10t`, and `/clock hopper 7.4s`.

## Materials and undo

If the player's main hand contains a solid, full, Redstone-conducting block that does not emit a signal, it is used for the platform/outer blocks. Otherwise white wool is used. `/clock undo` is an alias for undoing the latest shared world edit. Shared history restores block states, inventories, and other block-entity data.
