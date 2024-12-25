# grpc-storage
## Implementing a key-value storage with a gRPC API

---
```yaml
java: 21
tarantool-db: 3.2.x
cartridge-java: 0.13.2
```
---
### Local testing
First, you need `tt` cli tool: [download-link](https://www.tarantool.io/ru/doc/latest/tooling/tt_cli/installation/)
```bash
# 1. Clone repo
git clone https://github.com/alex-pvl/grpc-storage.git
```
```bash
# 2. Create tarantool directory
cd grpc-storage
mkdir tarantool && cd tarantool
tt init
cd instances.enabled && mkdir kv_storage && cd kv_storage
```
```yaml
# 3. Create config.yaml file with the following content
credentials:
  users:
    api_user:
      password: 'api_user'
      roles: [ super ]
      privileges:
        - permissions: [ read, write, execute ]
          spaces: [ kv ]

groups:
  group001:
    replicasets:
      replicaset001:
        instances:
          instance001:
            iproto:
              listen:
                - uri: '127.0.0.1:3301'
```
```yaml
# 4. Create instances.yml file with the following content
instance001:
```
```bash
# 5. Start tarantool single instance
cd ../.. && tt start kv_storage
```
```bash
# 6. Check status
tt status kv_storage
```
```bash
# 7. Connect to the instance
tt connect kv_storage:instance001
```
```lua
-- 8. Create space and index
box.schema.space.create('kv', { if_not_exists = true })

box.space.kv:format({
    { name = 'key', type = 'string' },
    { name = 'value', type = 'varbinary', is_nullable = true }
})

box.space.kv:create_index('primary_idx', {
    type = 'tree',
    parts = { 'key' },
    if_not_exists = true
})
```
```bash
# 9. Build & start application
cd .. && ./gradlew clean build && ./gradlew :service:bootRun
```
