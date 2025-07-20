# LightWeightRW

一个轻量级的资源世界插件，适用于 Paper/Spigot/Bukkit 服务器。

## 功能特性

### 🌍 资源世界管理
- 自动创建和管理独立的资源世界
- 支持自定义世界种子
- 随机传送到安全位置
- 传送冷却时间控制

### ⏰ 自动重置
- 支持定时自动重置（如每天 24:00）
- 支持手动重置命令
- 重置前多级警告提醒
- 重置时自动踢出玩家到主世界

### 🚫 传送门限制
- 默认禁用地狱传送门
- 默认禁用末地传送门
- 防止玩家意外进入其他维度

### 🌐 多语言支持
- 内置英文 (en_US) 和中文 (zh_CN)
- 支持玩家个人语言设置
- 易于扩展其他语言

### 🔧 模块化设计
- 松耦合的模块化架构
- 完善的权限节点系统
- 易于维护和扩展

## 安装要求

- **服务端**: Paper 1.20.2+ / Spigot 1.20.2+ / Bukkit 1.20.2+
- **Java**: Java 17+
- **Kotlin**: 1.9.20 (已包含在插件中)

## 安装方法

1. 下载最新版本的 `LightWeightRW.jar`
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 插件将自动生成配置文件

## 命令使用

### 基础命令
- `/rw` - 显示帮助信息
- `/rw help` - 显示帮助信息
- `/rw tp` - 传送到资源世界

### 管理员命令
- `/rw reset` - 手动重置资源世界
- `/rw reload` - 重载插件配置

### 语言命令
- `/rw lang <语言代码>` - 更改个人语言设置

### 命令别名
- `/resourceworld`
- `/lwrw`
- `/rw`

## 权限节点

### 用户权限
- `lwrw.use` - 基础使用权限
- `lwrw.use.tp` - 传送到资源世界的权限

### 管理员权限
- `lwrw.admin` - 管理员权限（包含所有权限）
- `lwrw.admin.reset` - 重置资源世界的权限
- `lwrw.admin.reload` - 重载配置的权限
- `lwrw.admin.bypass` - 绕过冷却时间和限制
- `lwrw.*` - 所有权限

## 配置文件

### config.yml

```yaml
# 世界设置
world:
  name: "resource_world"  # 资源世界名称
  seed: 0                  # 世界种子（0为随机）
  disable-nether: true     # 禁用地狱传送门
  disable-end: true        # 禁用末地传送门

# 传送设置
teleport:
  radius: 1000            # 随机传送半径
  cooldown: 300           # 传送冷却时间（秒）
  min-y: 64               # 最低传送高度
  max-y: 120              # 最高传送高度

# 重置设置
reset:
  auto-reset:
    enabled: true         # 启用自动重置
    time: "24:00"         # 重置时间（24小时制）
  warning-times:          # 警告时间（分钟）
    - 60
    - 30
    - 10
    - 5
    - 1
  kick-players: true      # 重置时踢出玩家

# 语言设置
language:
  default: "en_US"        # 默认语言
  per-player: true        # 启用玩家个人语言

# 调试模式
debug: false
```

## 语言文件

插件支持多语言，语言文件位于 `plugins/LightWeightRW/lang/` 目录下：

- `en_US.yml` - 英文
- `zh_CN.yml` - 中文

你可以添加更多语言文件，文件名格式为 `语言代码.yml`。

## 开发信息

- **作者**: Snowball_233
- **命名空间**: cc.vastsea
- **语言**: Kotlin
- **构建工具**: Maven
- **许可证**: MIT

## 构建项目

```bash
# 克隆项目
git clone https://github.com/Snowball-233/LightWeightRW.git
cd LightWeightRW

# 编译项目
mvn clean package

# 编译后的文件位于 target/ 目录
```

## 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 这个项目
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

## 更新日志

### v1.0.0
- 初始版本发布
- 基础资源世界功能
- 自动重置系统
- 多语言支持
- 传送门限制
- 完整的权限系统

## 支持

如果你遇到问题或有建议，请：

1. 查看 [Wiki](https://github.com/Snowball-233/LightWeightRW/wiki)
2. 提交 [Issue](https://github.com/Snowball-233/LightWeightRW/issues)
3. 加入我们的 Discord 服务器

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

**感谢使用 LightWeightRW！** 🎉