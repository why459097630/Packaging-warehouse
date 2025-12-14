# Public Surface Freeze (12 Components)

UI 包必须**完整实现并对外暴露**与 SST 一致的 12 类组件签名；任何新增/删除/改名都会被视为 breaking。

## 组件列表（逐项自查 ✓/✗）
- AppBar …… [  ]
- NavBar/Tabs …… [  ]
- Button …… [  ]
- Card …… [  ]
- ListItem …… [  ]
- FormField …… [  ]
- Chip …… [  ]
- Banner …… [  ]
- Empty …… [  ]
- Skeleton …… [  ]
- BottomSheet …… [  ]
- Toast …… [  ]

> 要求：props 名称、枚举取值、默认值均与 SST 一致；不可额外暴露不在 SST 的公开参数。
