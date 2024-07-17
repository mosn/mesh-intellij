package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class HashMapTemplate implements Template {

    public static final String Name = "hashmap.go";

    public static final String Path = "pkg/common/safe";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.header();

        // write package and import

        buffer.line()
                .append("package safe").line()
                .append("import (")
                .append("\t\"errors\"")
                .append("\t\"fmt\"")
                .append("\t\"sync\"")
                .append(")").line();

        // write code

        buffer.append("// IntMap Used to store type mappings of string and uint64 and is thread safe.")
                .append("// This is especially useful in protocol scenarios where string ID identifiers are used")
                .append("type IntMap struct {")
                .append("	table map[string]uint64 // id -> encoded stream id")
                .append("	lock  sync.RWMutex      // protect table")
                .append("}")
                .line();


        buffer.append("func (m *IntMap) Get(key string) (val uint64, found bool) {").line()
                .append("	m.lock.RLock()")
                .append("	if len(m.table) <= 0 {")
                .append("		m.lock.RUnlock() // release read lock.")
                .append("		return 0, false")
                .append("	}")
                .line()
                .append("	val, found = m.table[key]")
                .line()
                .append("	m.lock.RUnlock()")
                .append("	return")
                .append("}")
                .line();


        buffer.append("func (m *IntMap) Put(key string, val uint64) (err error) {").line()
                .append("	m.lock.Lock()")
                .append("	if m.table == nil {")
                .append("		m.table = make(map[string]uint64, 8)")
                .append("	}")
                .line()
                .append("	if v, found := m.table[key]; found {")
                .append("		m.lock.Unlock()")
                .append("		return errors.New(fmt.Sprintf(\"val conflict, exist key %s, val %d, current %d\", key, v, val))")
                .append("	}")
                .line()
                .append("	m.table[key] = val")
                .append("	m.lock.Unlock()")
                .append("	return")
                .append("}")
                .line();

        buffer.append("func (m *IntMap) Remove(key string) (err error) {").line()
                .append("	m.lock.Lock()")
                .append("	if m.table != nil {")
                .append("		delete(m.table, key)")
                .append("	}")
                .line()
                .append("	m.lock.Unlock()")
                .append("	return")
                .append("}").line();

        Content = buffer.toString();
    }

    public static Source hashMap() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(hashMap());
    }
}
