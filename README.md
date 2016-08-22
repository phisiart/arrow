# Arrow

Constructing Dataflow Graphs with Function Composition Style

## Introduction

Arrow is a domain-specific language enabling users to create dataflow graphs easily in a declarative way.

## Roadmap

Arrow is being reconstructed and open-sourced. The following features are to be (re)implemented.

- [x] Type Checking
- [ ] Graph Constructing
- [ ] Graph Drawing
- [ ] Parallel Runtime
- [ ] Distributed Execution Support

## Connection Examples
### A Simple Flow
```scala
val producer: Node[Int, Int] = ...
// or (function instead of node):
// val producer: Int => Int = ...

val consumer: Node[Int, Int] = ...
// or (function instead of node):
// val consumer: Int => Int = ...

val flow = Stream(1, 2, 3) |> producer |> consumer
// or (associativity):
// val flow = Stream(1, 2, 3) |> (producer |> consumer)
// or (right to left):
// val flow = consumer <| producer <| Stream(1, 2, 3)
```

### Broadcast
```scala
// `_` means the type is unimportant here, similarly hereinafter
// `I => O` can also be `Node[I, O]`, similarly hereinafter
// `List` can also be any subtype of `Traversable`, similarly hereinafter
val producer: _ => Int = ...
val consumers: List[Int => _] = ...
producer |> consumers
```

### Collect
```scala
val producers: Vector[_ => Int] = ...
val consumer: Int => _ = ...
producers |> consumer
```

### Split
```scala
val producer: _ => List[Int] = ...
val consumers: List[Int => _] = ...
producer |> consumers
```

### Join
```scala
val producers: List[_ => Int] = ...
val consumer: List[Int] => _ = ...
producers |> consumer
```

### HSplit
```scala
import shapeless._
val producer: _ => (Int :: Double :: HNil)
val consumers: (Int => _) :: (Double => _) :: HNil
producer |> consumers
```

### HJoin
```scala
import shapeless._
val producers: (_ => Int) :: (_ => Double) :: HNil
val consumer: (Int :: Double :: HNil) => _
producers |> consumer
```

### HMatch
```scala
import shapeless._ // for `HList`
val producers: (_ => Int) :: (_ => Double) :: HNil
val consumers: (Int => _) :: (Double => _) :: HNil
produceres |> consumers
```

### Using Flows as Inputs/Outputs
```scala
import shapeless._
val flow_lhs: _ => String = ...
val flow_rhs: String => Int = ...
val flow = flow_lhs |> flow_rhs // inputs `_`, outputs `Int`

val producers                     = flow :: xx :: HNil
val consumers: (Int => _) :: HNil = ...

producers |> consumers
```