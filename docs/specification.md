# Safe Railway-crossing for autonomous vehicles
Build a smart contract that manages the crossing permissions of autonomous vehicles and trains through a railway crossing.
## Actors

* Railway infrastructure managers
* Trains
* Autonomous vehicles

## Entities

### Railway crossing

| id     | laneIds | state                   | prioritylock      | validuntil |
|--------|---------|-------------------------|-------------------|------------|
| string | string  | "LOCKED"/"FREE TO CROSS"| boolean           | long       |

- id is a  unique identifier of the crossing
- laneIds are the lanes belonging to the crossing
- state specifies whether the crossing can be crossed freely by the train
- prioritylock is true when a train has requested permission to pass
- validuntil is a timestamp that determines the validity of the free to cross state

### Lane

| id     | crossingid | capacity | occupied | prioritylock |
|--------|------------|----------|----------|--------------|
| string | string     | int      | int      | boolean      |

- id in combination with the crossingid uniquely identifies the lane
- capacity is the number of cars that can use the lane at the same time
- occupied is the number of cars currently using the lane
- prioritylock is true when a train has requested permission to pass the crossing the lane belongs to.

### CrossingPermission
| id     | crossingid | identity | released |
|--------|------------|----------|----------|
| string | string     | string   | boolean  |

- id in combination with the crossingid uniquely identifies the CrossingRequest
- indentity is a sha256sum hash of the request's sender identity
- isgranted signifies whether the the permission to cross was denied or granted 
- released signifies whether or not the pkkk

## State Machines

### System overview

The diagram below depicts the main behaviour of the system. The 
periodic refreshments of free crossing validity have been omitted to improve understandability,
but after a predetermined duration has passed in any of the FreeForTrain states,
the invalid variable becomes true, and the active state transitions to the LockedForTrain states. 
From there if the Validate signal is triggered the state can transition back to the FreeForTrain state.

```plantuml
hide empty description
state PriorityLocked {
    
    
    state LockedForTrain2{
        
    }
    state FreeForTrain2
    state LockTrainChoice <<choice>>
    LockedForTrain2 --> LockTrainChoice: CrossingRelease
    LockTrainChoice --> LockedForTrain2: [freeLanes<lanes]
    LockTrainChoice --> FreeForTrain2: [freeLanes=lanes]
    FreeForTrain2 --> LockedForTrain2: [invalid]
    LockedForTrain2 --> FreeForTrain2: Validate
}

state PriorityFree {
    [*] --> FreeForTrain1
    
    state FreeForTrain1
    
    note left of FreeForTrain1
    Free signal for
    both trains and
    road vehicles
    endnote
    
    state LockedForTrain1{
    	    
    	state FreeLanesChoice <<choice>>
    	[*] --> Free
    	Free --> FreeLanesChoice: CrossingRequest /grant
    	Free --> Free: CrossingRelease
    	FreeLanesChoice --> Locked: [freeLanes=0]
    	FreeLanesChoice --> FreeForTrain1: [freeLanes=lanes]
    	FreeLanesChoice --> Free: [freeLanes>0]
    	Locked --> Free: CrossingRelease
    	Locked --> Locked: CrossingRequest /deny
        
    }
    FreeForTrain1 --> FreeForTrain1: CrossingRelease
    FreeForTrain1 --> LockedForTrain1: CrossingRequest /grant
    FreeForTrain1 --> LockedForTrain1: [invalid]
    FreeForTrain1 --> FreeForTrain2: TrainRequest
    LockedForTrain1 --> FreeForTrain1: Validate
    LockedForTrain1 --> LockedForTrain2: TrainRequest
        
}

[*] --> PriorityFree
PriorityLocked --> PriorityFree[H*] : TrainRelease
PriorityFree --> PriorityFree : TrainRelease

PriorityLocked --> PriorityLocked: CrossingRequest /deny

````
## Lanes
An intersection can contain one or more lanes, each of which can serve
one or more cars at once. The amount is determined by the railroad infrastructure management. The diagram below shows the behaviour of each lane.

```plantuml
hide empty description
state free
state closed
state choice <<choice>>
[*] --> free
free --> free: CrossingRelease
free --> choice: CrossingRequest /grant
choice --> free: [occupancy<capacity]
choice --> closed: [occupancy=>capacity]
closed --> closed: CrossingRequest /deny
closed --> free: CrossingRelease
```