### xframe-jdbc
```
* jdbctemplate
* typequery (type & table binding)
* sqlscript (tools)
```
#### setup
```java
JdbcEnviron.getConfigurator()
	.setFieldMapper(mapper)
	.setFieldCodec(codec)
	.setAsyncThreads(nthreads)
	.setDelimiters(major, minor)
	.setInstupUsage(_insert, _update)
	.setDataSource(dbkey, dataSource)
	
//default supported jdbc pool
dataSource = DataSources.tomcatjdbc(dbconf)
```
#### usage
```java
TypeQuery<T> query = TypeQuery.newBuilder(T.class)
							.setTable(dbkey, tableName)
							.setFieldCodec(fieldName, codec)
							.setTypeHandler(typeHandler)
							.setSQL(0, TypeSQL.where().EQ("id").select())
							.setAsyncModel(false)
							.setInstupUsage(0)
							.build()

T one = query.fetchOne(PSSetter.of(key))
T one = query.getSQL(0).fetchone(PSSetter.of(id))

List<T> list = query.fetchall()

query.update(T)
query.insert(T)
query.instup(T) 	(insert on duplicate key update)
```
#### tools
```
SQLDiff.diff(base_dbconf, curr_dbconf)
SQLScript.parse(File/Text)
```