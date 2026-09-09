# The spell system

One vector in, something the world does out.

`net.fahr3n.unnecessarilycompressedcobblestone.magic` takes a **spell output vector** — fifteen
signed numbers, one per property axis — and resolves it into Minecraft: a projectile in flight, a
wall of ice, an aura, a buff on the caster, a summoned creature, damage, statuses, boons. Nothing in
it names a spell, and no spell names anything in it.

**This package does not generate vectors.** Where the numbers come from is deliberately somebody
else's problem — a circuit upstream, `/uccspell` for testing, whatever comes later. The seam is one
record with a map of doubles in it.

## Trying it

```
/uccspell fixtures                    # every test vector, and what it resolves to
/uccspell fixture fireball            # cast one
/uccspell fixture arrow               # ...and watch the same code behave nothing alike
/uccspell read agitation=8,vector=5   # resolve a vector and print it without casting
/uccspell cast agitation=8,vector=5,cohesion=-2 4 8      # vector, quality, quantity
```

`read` is the more useful half: it prints the delivery, the payload list with amounts and durations,
and every delivery's score, which is how the numbers below were tuned.

## The two halves

| | Choice | Why |
|---|---|---|
| **Delivery** — how it arrives | **exclusive**, so scored | A spell cannot be both a bolt in flight and a wall of ice. Rows compete; highest score wins. |
| **Payload** — what it does | **inclusive**, so filtered | A spell can burn *and* freeze *and* slow. Every row whose condition passes fires. |

That asymmetry is the load-bearing decision. Trying to make delivery compose, or payload exclusive,
is what makes the problem look combinatorial when it is not.

### Deliveries — `SpellDelivery`

| Row | Reads | Becomes |
|---|---|---|
| `projectile` | push, manifest | one `SpellProjectileEntity` |
| `placed` | bind, ordered, manifest, **not** coupled | blocks, via `DeferredFill` |
| `aura` | endures, reaches, going nowhere | payloads on everything in radius |
| `self` | **coupled** | payloads on the caster |
| `summon` | alive **and** bound | a creature, from `SpellSummons` |
| `dissipate` | nothing in particular | the sound and the puff, and nothing else |

Three of those rows are worth their comments in the source:

- `placed` is penalised by the coupling pole as hard as by the pushing pole. Matter bound in front of
  you is a barrier; the identical matter bound *and coupled to somebody's own magic* is armour. That
  one axis is the whole difference between a wall and a ward, and without the penalty they are the
  same vector.
- `summon` is the only score built on a **minimum** rather than a sum, because "alive *and* bound" is
  not something a weighted sum can say — it would let a great deal of one stand in for none of the
  other, and every strong healing vector would summon something.
- `dissipate` sits at a constant floor, which is what makes the table total. Once shape decides
  everything, most conceivable shapes decide on nothing in particular. A cast that earned no
  delivery still cost its caster the mana, so it gets feedback and lands nothing — a silent no-op
  reads as a bug.

### Payloads — `SpellPayload`

Twenty-one rows: five damage, six status, six boon, four utility. Each is a condition on the
*shares* of the vector, an amount, and a duration.

**A fireball cannot paralyse.** Paralysis is written as entropy delivered through something that
permeates; a fireball carries structure and manifests as matter, so it fails the condition twice
over. Flip those two poles and the same circuit paralyses, having never mentioned it. That is the
whole argument for a condition table over a switch on spell ids, and it is why a spell nobody has
written yet resolves correctly.

Amounts are always `ceiling × intensity × some shares`, and `intensity` is `x / (x + 24)` — bounded
by construction, so no arriving vector however deep produces an absurd number. Durations are bought
on the enduring pole and on how much mana was fed in, never on quality: **how good the mana was is
how hard it hits, how much of it there was is how long it lasts.**

### Damage goes through the damage type registry

`ModDamageTypes` registers five real types (impact, thermal, frost, shear, shock) as datapack data,
so armour, Protection, Resistance, the totem, death messages, advancement triggers and every other
mod's damage reduction all apply with no code here. `ModDamageTypeTagProvider` puts frost in
`#is_freezing`, shock in `#is_lightning`, thermal in `#is_fire` and impact in `#is_projectile` —
which is what makes this mod's own lightning conjurer immune to a shock spell without the magic
package knowing that creature exists.

**Every damage row that fires is summed into one `hurt` call.** A living entity is invulnerable for
twenty ticks after being hurt unless the next hit is larger, so five separate calls would land the
biggest and silently discard the other four. The sum is credited to whichever type contributed most.

### The caster

In range like anything else, and spared only the damage. One rule instead of two: a helpful aura
reaches the person who cast it, a cold field slows them as it slows everyone, and their own fireball
does not burn them. A self-cast is the deliberate exception — it is not spared, because a spell aimed
at yourself was aimed at yourself.

### The lookup tables

`SpellSubstance` and `SpellSummons` answer the two questions a vector cannot: what the matter turned
out to be, and what answered the summoning. Both score every row against the shape and take the
highest, both end in a row that wins when nothing else can, and neither is reachable from anything
upstream. Adding a block or a creature to the mod's reach is a row and nothing else.

Whose side a summon is on is not a setting — it is which row won. Allied rows read the coupling pole
with real weight and wild rows read none of it, so a caster who spent nothing on coupling to what
they were making gets a zombie, and one who spent on it gets a wolf.

### Looks

`SpellAppearance` derives a particle, a colour and a sound from the same shares the payload table
reads. Nothing is chosen by name: a hot, bright, loosely bound emission arrives as flame whether or
not anything called it fire, and the tinted-dust fallback means there is no such thing as an
invisible cast.

### Bodies — `SpellForm`, `SpellSkin`, `SpellLook`

A spell is also a **thing that is there**: a real entity, shaped at spawn time out of the vector.

- **`SpellForm`** — what shape it is. Scored and exclusive, like a delivery, with `MOTE` at a floor
  so the table is total. A row is not a model (models are baked per entity type, before any vector
  exists) — it carries a `Mesh` out of a small primitive vocabulary and the numbers that shape it.
- **`SpellSkin`** — what its surface is. Scored, with `ARCANE` at a floor. Every sheet is
  **greyscale**: colour is continuous and arrives per vertex from `SpellAppearance.colour`, so eight
  textures cover every colour a vector can imply. Baking hues in would need a texture per shade.
- **`SpellLook`** — the two enums plus the continuous half (size, stretch, spin, wobble, alpha, tint)
  packed into ~20 bytes. Derived once on the **server** and sent in the spawn packet; the client is
  handed the answer rather than fifteen doubles it would have to re-derive in a second place that
  could drift.

Two entities carry one: `SpellProjectileEntity` (in flight; its hitbox follows its look, so a spell
that reads as a cannonball hits like one) and `SpellBodyEntity` (standing still — an aura, a wall
being built, a summoning). Both are drawn by `SpellVisualRenderer`, which is written against the
`SpellVisual` interface and not against either of them, and which builds every quad at draw time —
the same way vanilla draws the end crystal's beam.

Two things carry the "this is glowing" read, and both are needed because none of the render types is
additive — alpha blending can only darken what is behind it. Every spell is drawn **emissive** unless
its vector is explicitly about darkness, and every form but `SHELL` draws a second smaller near-opaque
pass inside itself, whitened towards its own colour. A lone translucent surface with world lighting on
it is a bead of tinted glass; those two turn it into a thing with a hot middle.

**Particles were not replaced, they were divided from.** An entity is tracked and packeted, so there
is one per cast; particles are client side and free, so there can be forty. Bodies are the shape;
particles remain the trail and the burst. One entity per puff would be strictly worse than what the
particles already do.

`/uccspell read` prints the form and skin alongside the delivery, so both tables retune off one
command.

## Adding to it

| To add | Change |
|---|---|
| a new thing spells can do | a row in `SpellPayload` |
| a new way spells can arrive | a row in `SpellDelivery` |
| a new material a spell can make | a row in `SpellSubstance` |
| a new creature a spell can call | a row in `SpellSummons` |
| a new shape a spell can take | a row in `SpellForm` — and a `Mesh` case only if it needs a new primitive |
| a new surface a spell can have | a row in `SpellSkin` and a PNG from `tools/build_spell_skins.py` |
| a new kind of spell entity | implement `SpellVisual`, register `SpellVisualRenderer` — no model, no texture |
| a new kind of bolt | **nothing** — it is a vector |

## Tuning

The thresholds and weights are hand-tuned against the thirteen fixtures in `SpellCommand`, checked
by resolving all of them and confirming each lands on its intended delivery with every row of the
table exercised. That check is arithmetic on the shares and needs no game running; the fixtures exist
so it can be repeated after any weight moves.

The axes carrying the least tuning are Resonance and Vitality, which the upstream design leaves
mechanically unread — the `self` and `summon` rows lean on them hardest and are the ones to expect to
retune first.

## What is not built

- No vector generator. By design.
- `aura` hands out real effect durations, which vanilla syncs, saves and expires; the
  `SpellBodyEntity` standing in it is scenery and does nothing. Nothing about a cast depends on a
  body existing.
- A body does not follow anything. A self-cast's body marks where the caster stood when the spell
  took hold and then fades; it does not trail a walking player.
- Nothing consumes anything. There is no mana pool here, no cost and no refusal — a cast always
  happens. Pricing belongs to whatever produces the vector.
- Perception is read only as a small widening of radius, and Range only as radius and structure size.
