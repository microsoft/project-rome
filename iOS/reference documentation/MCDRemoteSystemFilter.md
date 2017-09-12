# protocol `MCDRemoteSystemFilter`

```
@protocol MCDRemoteSystemFilter
```

Set of methods to be implemented by objects acting as remote system discovery filters.

## Summary

 Members                        | Descriptions                                
--------------------------------|---------------------------------------------
matchesRemoteSystem | Checks whether a remote system passes the current filter.

## Methods

### matchesRemoteSystem
`-(BOOL)matchesRemoteSystem:(nonnull MCDRemoteSystem*)remoteSystem;`

Checks whether a remote system passes the current filter.

#### Parameters
* `remoteSystem` The remote system to check.

#### Returns
**TRUE** if the given remote system passes through the filter, otherwise **FALSE**.
