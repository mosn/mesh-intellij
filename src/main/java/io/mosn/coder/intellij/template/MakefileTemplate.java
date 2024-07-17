package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class MakefileTemplate implements Template {

    public static final String Name = "Makefile";

    public static final String Path = "";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        // write package and import

        buffer.append("SHELL\t\t\t= /bin/bash")
                .append("TARGET\t\t\t= ${plugin}")
                .append("OUTPUT\t\t\t= ${TARGET}.so")
                .append2("ARM_TO_AMD\t\t= ${arm.to.amd}")

                .append("STREAM_FILTER = ${filter}")
                .append2("TRANSCODER = ${trans}")

                .append("CODEC_PREFIX\t= codec")
                .append("CODEC_OUTPUT\t= ${CODEC_PREFIX}-${OUTPUT}")
                .append("CODEC_ZIP_OUTPUT = ${TARGET}.zip")
                .append2("TRANSCODER_ZIP_OUTPUT = ${TARGET}.zip")

                .append("PLUGIN_GOOS = ${GOOS}")
                .append2("PLUGIN_GOARCH = ${GOARCH}")

                .append("TRANSCODER_PREFIX = transcoder")
                .append2("TRANSCODER_OUTPUT = ${TRANSCODER_PREFIX}-${OUTPUT}")

                .append2("STEAM_FILTER_PREFIX = filter")

                .append2("TRACE_PREFIX = trace")

                .append("GIT_VERSION \t\t\t= $(shell git log -1 --pretty=format:%H | head -c 8)")
                .append("PROJECT_NAME\t\t\t= $(shell head -1 go.mod | cut  -d' ' -f2)")
                .append2("FULL_PROJECT_NAME\t\t= $(shell pwd)")

                .append("# only for local test")
                .append2("SIDECAR_GITLAB_PROJECT_NAME = gitlab.alipay-inc.com/ant-mesh/mosn")

                .append("SIDECAR_BUILD_IMAGE     = mosn-builder")
                .append("BUILD_IMAGE\t\t\t\t= golang:1.20.1")
                .append2("BASE_IMAGE\t\t\t\t= zonghaishang/delve:v1.20.1")

                .append2("os_arch:=$(shell uname)_$(shell arch)")

                // target: codec
                .append("codec:")
                .append2("\tmake codec.arch")

                // target: codec.amd64:
                .append("codec.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make codec.arch")

                // target: codec.arm64:
                .append("codec.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make codec.arch")

                // target: filter
                .append("filter: # alias for stream-filter (more short)")
                .append2("\tmake stream-filter.arch")

                // target: filter.amd64
                .append("filter.amd64:")
                .append2("\tmake stream-filter.amd64")

                // target: filter.arm64
                .append("filter.arm64:")
                .append2("\tmake stream-filter.arm64")

                // target: stream-filter.amd64
                .append("stream-filter.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make stream-filter.arch")

                // target: stream-filter.arm64
                .append("stream-filter.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make stream-filter.arch")

                // target: trans
                .append("trans: # alias for transcoder (more short)")
                .append2("\tmake transcoder.arch")

                // target: trans.amd64
                .append("trans.amd64:")
                .append2("\tmake transcoder.amd64")

                // target: trans.arm64
                .append("trans.arm64:")
                .append2("\tmake transcoder.arm64")

                // target: transcoder.amd64
                .append("transcoder.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make transcoder.arch")

                // target: transcoder.arm64
                .append("transcoder.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make transcoder.arch")

                // target: trace
                .append("trace:")
                .append("\tmake trace.arch")
                .line()

                // target: trace.amd64
                .append("trace.amd64:")
                .append("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make trace.arch")
                .line()

                // target: trace.arm64
                .append("trace.arm64:")
                .append("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make trace.arch")
                .line()

                // target: ant
                .append("ant:")
                .append2("\tmake ant.arch")

                // target: ant.amd64
                .append("ant.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make ant.arch")

                // target: ant.arm64
                .append("ant.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make ant.arch")

                // target: pkg-codec
                .append("pkg-codec:")
                .append("ifeq (\"$(os_arch)\",\"Darwin_arm64\")")
                .append("ifeq (\"$(ARM_TO_AMD)\",\"off\")")
                .append("\tmake pkg-codec.arm64")
                .append("else")
                .append("\tmake pkg-codec.amd64")
                .append("endif")
                .append("else ifeq (\"$(os_arch)\",\"Darwin_i386\")")
                .append("\tmake pkg-codec.amd64")
                .append("else")
                .append("\tmake pkg-codec.arch")
                .append2("endif")

                // target: pkg-codec.amd64
                .append("pkg-codec.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make pkg-codec.arch")

                // target: pkg-codec.arm64
                .append("pkg-codec.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make pkg-codec.arch")

                // target: pkg-filter
                .append("pkg-filter:")
                .append("ifeq (\"$(os_arch)\",\"Darwin_arm64\")")
                .append("ifeq (\"$(ARM_TO_AMD)\",\"off\")")
                .append("\tmake pkg-filter.arm64")
                .append("else")
                .append("\tmake pkg-filter.amd64")
                .append("endif")
                .append("else ifeq (\"$(os_arch)\",\"Darwin_i386\")")
                .append("\tmake pkg-filter.amd64")
                .append("else")
                .append("\tmake pkg-filter.arch")
                .append2("endif")

                // target: pkg-filter.amd64
                .append("pkg-filter.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make pkg-filter.arch")

                // target: pkg-filter.arm64
                .append("pkg-filter.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make pkg-filter.arch")

                // target: pkg-trans
                .append("pkg-trans:")
                .append("ifeq (\"$(os_arch)\",\"Darwin_arm64\")")
                .append("ifeq (\"$(ARM_TO_AMD)\",\"off\")")
                .append("\tmake pkg-trans.arm64")
                .append("else")
                .append("\tmake pkg-trans.amd64")
                .append("endif")
                .append("else ifeq (\"$(os_arch)\",\"Darwin_i386\")")
                .append("\tmake pkg-trans.amd64")
                .append("else")
                .append("\tmake pkg-trans.arch")
                .append2("endif")

                // target: pkg-trans.amd64
                .append("pkg-trans.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make pkg-trans.arch")

                // target: pkg-trans.arm64
                .append("pkg-trans.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make pkg-trans.arch")

                // target: pkg-trace:
                .append("pkg-trace:")
                .append("ifeq (\"$(os_arch)\",\"Darwin_arm64\")")
                .append("ifeq (\"$(ARM_TO_AMD)\",\"off\")")
                .append("\tmake pkg-trace.arm64")
                .append("else")
                .append("\tmake pkg-trace.amd64")
                .append("endif")
                .append("else ifeq (\"$(os_arch)\",\"Darwin_i386\")")
                .append("\tmake pkg-trace.amd64")
                .append("else")
                .append("\tmake pkg-trace.arch")
                .append2("endif")

                // target: pkg-trace.amd64
                .append("pkg-trace.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make pkg-trace.arch")

                // target: pkg-trace.arm64
                .append("pkg-trace.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make pkg-trace.arch")

                // target: pkg-ant
                .append("pkg-ant:")
                .append2("\tmake pkg-ant.arch")

                // target: pkg-ant.amd64
                .append("pkg-ant.amd64:")
                .append2("\tGOOS=linux GOARCH=amd64 DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" make pkg-ant.arch")

                // target: pkg-ant.arm64
                .append("pkg-ant.arm64:")
                .append2("\tGOOS=linux GOARCH=arm64 DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" make pkg-ant.arch")

                // target: start
                .append("start:")
                .append("    ifeq (\"$(os_arch)\",\"Darwin_arm64\")")
                .append("\t\tmake start.arm64")
                .append("    else")
                .append("\t\tmake start.amd64")
                .append2("    endif")

                // target: start.amd64
                .append("start.amd64:")
                .append("\tFULL_PROJECT_NAME=${FULL_PROJECT_NAME} PROJECT_NAME=${PROJECT_NAME}  DOCKER_BUILD_OPTS=\"--platform=linux/amd64\" \\")
                .append2("\tSIDECAR_GITLAB_PROJECT_NAME=${SIDECAR_GITLAB_PROJECT_NAME} bash ${FULL_PROJECT_NAME}/etc/ant/start.sh")

                // target: start.arm64
                .append("start.arm64:")
                .append("\tFULL_PROJECT_NAME=${FULL_PROJECT_NAME} PROJECT_NAME=${PROJECT_NAME} DOCKER_BUILD_OPTS=\"--platform=linux/arm64\" \\")
                .append2("    SIDECAR_GITLAB_PROJECT_NAME=${SIDECAR_GITLAB_PROJECT_NAME} bash ${FULL_PROJECT_NAME}/etc/ant/start.sh")

                // target: debug
                .append("debug:")
                .append2("\tDLV_DEBUG=true make start")

                // target: stop
                .append("stop:")
                .append2("\tbash ${FULL_PROJECT_NAME}/etc/ant/stop.sh")

                // target: clean
                .append("clean:")
                .append2("\t@rm -rf build/codecs build/stream_filters build/transcoders build/traces build/target")

                // target: codec.arch
                .append("codec.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_TARGET=${TARGET} \\")
                .append("\t-e PLUGIN_CODEC_OUTPUT=${CODEC_OUTPUT} \\")
                .append("\t-e PLUGIN_CODEC_PREFIX=${CODEC_PREFIX} \\")
                .append("    -e PLUGIN_STEAM_FILTER_PREFIX=${STEAM_FILTER_PREFIX} \\")
                .append("    -e PLUGIN_TRANSCODER_PREFIX=${TRANSCODER_PREFIX} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_STREAM_FILTER=${STREAM_FILTER} \\")
                .append("\t-e PLUGIN_TRANSCODER=${TRANSCODER} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BUILD_IMAGE} \\")
                .append2("\tbash /go/src/${PROJECT_NAME}/etc/script/compile-codec.sh")

                // target: stream-filter.arch
                .append("stream-filter.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_STREAM_FILTER=${TARGET} \\")
                .append("\t-e PLUGIN_STEAM_FILTER_PREFIX=${STEAM_FILTER_PREFIX} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t-v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BUILD_IMAGE} \\")
                .append2("\tbash /go/src/${PROJECT_NAME}/etc/script/compile-filter.sh")

                // target: transcoder.arch
                .append("transcoder.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_TRANSCODER=${TARGET} \\")
                .append("\t-e PLUGIN_TRANSCODER_PREFIX=${TRANSCODER_PREFIX} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t -v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BUILD_IMAGE} \\")
                .append2("\tbash /go/src/${PROJECT_NAME}/etc/script/compile-transcoder.sh")

                // target: trace.arch:
                .append("trace.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_TRACE=${TARGET} \\")
                .append("\t-e PLUGIN_TRACE_PREFIX=${TRACE_PREFIX} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t -v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BUILD_IMAGE} \\")
                .append2("\tbash /go/src/${PROJECT_NAME}/etc/script/compile-trace.sh")

                // target: compile-codec
                .append("compile-codec:")
                .append("\t@rm -rf build/codecs/${PLUGIN_TARGET}")
                .append("\tGO111MODULE=on ${BUILD_OPTS} go build \\")
                .append("\t\t--buildmode=plugin -mod=mod \\")
                .append("\t\t-gcflags \"all=-N -l\" \\")
                .append("\t\t-o ${PLUGIN_CODEC_OUTPUT} ${PLUGIN_PROJECT_NAME}/plugins/codecs/${PLUGIN_TARGET}/main")
                .append("\tmkdir -p build/codecs/${PLUGIN_TARGET}")
                .append("\tmv ${PLUGIN_CODEC_OUTPUT} build/codecs/${PLUGIN_TARGET}")
                .append("\t@cd build/codecs/${PLUGIN_TARGET} && $(shell which md5sum) -b ${PLUGIN_CODEC_OUTPUT} | cut -d' ' -f1  > ${PLUGIN_CODEC_PREFIX}-${PLUGIN_TARGET}.md5")
                .append("\tcp configs/codecs/${PLUGIN_TARGET}/*.json build/codecs/${PLUGIN_TARGET}")
                .append2("\t@echo \"compile codec ${PLUGIN_TARGET} complete\"")

                // target: compile-stream-filter
                .append("compile-stream-filter:")
                .append("\t@rm -rf build/stream_filters/${PLUGIN_TARGET}")
                .append("\tGO111MODULE=on ${BUILD_OPTS} go build \\")
                .append("\t\t--buildmode=plugin -mod=mod \\")
                .append("\t\t-gcflags \"all=-N -l\" \\")
                .append("\t\t-o ${PLUGIN_STEAM_FILTER_OUTPUT} ${PLUGIN_PROJECT_NAME}/plugins/stream_filters/${PLUGIN_TARGET}/main")
                .append("\tmkdir -p build/stream_filters/${PLUGIN_TARGET}")
                .append("\tmv ${PLUGIN_STEAM_FILTER_OUTPUT} build/stream_filters/${PLUGIN_TARGET}")
                .append("\t@cd build/stream_filters/${PLUGIN_TARGET} && $(shell which md5sum) -b ${PLUGIN_STEAM_FILTER_OUTPUT} | cut -d' ' -f1  > ${PLUGIN_STEAM_FILTER_PREFIX}-${PLUGIN_TARGET}.md5")
                .append("\tcp configs/stream_filters/${PLUGIN_TARGET}/*.json build/stream_filters/${PLUGIN_TARGET}")
                .append2("\t@echo \"compile filter ${PLUGIN_TARGET} complete\"")

                // target: compile-transcoder
                .append("compile-transcoder:")
                .append("\t@rm -rf build/transcoders/${PLUGIN_TARGET}")
                .append("\tGO111MODULE=on ${BUILD_OPTS} go build \\")
                .append("\t\t--buildmode=plugin -mod=mod \\")
                .append("\t\t-gcflags \"all=-N -l\" \\")
                .append("\t\t-o ${PLUGIN_TRANSCODER_OUTPUT} ${PLUGIN_PROJECT_NAME}/plugins/transcoders/${PLUGIN_TARGET}/main")
                .append("\tmkdir -p build/transcoders/${PLUGIN_TARGET}")
                .append("\tmv ${PLUGIN_TRANSCODER_OUTPUT} build/transcoders/${PLUGIN_TARGET}")
                .append("\t@cd build/transcoders/${PLUGIN_TARGET} && $(shell which md5sum) -b ${PLUGIN_TRANSCODER_OUTPUT} | cut -d' ' -f1  > ${PLUGIN_TRANSCODER_PREFIX}-${PLUGIN_TARGET}.md5")
                .append("\tcp configs/transcoders/${PLUGIN_TARGET}/*.json build/transcoders/${PLUGIN_TARGET}")
                .append2("\t@echo \"compile transcoder ${PLUGIN_TARGET} complete\"")

                // target: compile-trace
                .append("compile-trace:")
                .append("\t@rm -rf build/traces/${PLUGIN_TARGET}")
                .append("\t@rm -rf build/codecs/bundle/support/traces/${PLUGIN_TARGET}")
                .append("\tGO111MODULE=on ${BUILD_OPTS} go build \\")
                .append("\t\t--buildmode=plugin -mod=mod \\")
                .append("\t\t-gcflags \"all=-N -l\" \\")
                .append("\t\t-o ${PLUGIN_TRACE_OUTPUT} ${PLUGIN_PROJECT_NAME}/plugins/traces/${PLUGIN_TARGET}/main")
                .append("\t@mkdir -p build/traces/${PLUGIN_TARGET}")
                .append("\tmkdir -p build/codecs/bundle/support/traces/${PLUGIN_TARGET}")
                .append("\t@mv ${PLUGIN_TRACE_OUTPUT} build/traces/${PLUGIN_TARGET}")
                .append("\t@cd build/traces/${PLUGIN_TARGET} && $(shell which md5sum) -b ${PLUGIN_TRACE_OUTPUT} | cut -d' ' -f1  > ${PLUGIN_TRACE_PREFIX}-${PLUGIN_TARGET}.md5")
                .append("\t@cp configs/traces/${PLUGIN_TARGET}/*.json build/traces/${PLUGIN_TARGET}")
                .append("\tcp -r build/traces/${PLUGIN_TARGET} build/codecs/bundle/support/traces/")
                .append2("\t@echo \"compile trace ${PLUGIN_TARGET} complete\"")

                // target: ant.arch
                .append("ant.arch:")
                .append("\tbash ${FULL_PROJECT_NAME}/etc/ant/stop.sh")
                .append("\t@test ! -f ~/.netrc  && touch ~/.netrc || true")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e SIDECAR_PROJECT_NAME=${SIDECAR_GITLAB_PROJECT_NAME} \\")
                .append("\t-v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v ~/.netrc:/root/.netrc \\")
                .append("\t-v $(shell go env GOPATH)/src/${SIDECAR_GITLAB_PROJECT_NAME}:/go/src/${SIDECAR_GITLAB_PROJECT_NAME} \\")
                .append("\t-w /go/src/${SIDECAR_GITLAB_PROJECT_NAME} ${BUILD_IMAGE} \\")
                .append2("\tbash /go/src/${PROJECT_NAME}/etc/script/compile.sh")

                // target: pkg-codec.arch
                .append("pkg-codec.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_CODEC_PREFIX=${CODEC_PREFIX} \\")
                .append("\t-e PLUGIN_CODEC=${TARGET} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM=$(shell uname) \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM_ARCH=$(shell arch) \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BASE_IMAGE} \\")
                .append("\tbash /go/src/${PROJECT_NAME}/etc/script/package-codec.sh")
                .append2("\t@echo \"package codec ${TARGET} complete\"")

                // target: pkg-filter.arch
                .append("pkg-filter.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_STREAM_FILTER=${TARGET} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_STEAM_FILTER_PREFIX=${STEAM_FILTER_PREFIX} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM=$(shell uname) \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM_ARCH=$(shell arch) \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BASE_IMAGE} \\")
                .append("\tbash /go/src/${PROJECT_NAME}/etc/script/package-filter.sh")
                .append2("\t@echo \"package filter ${TARGET} complete\"")

                // target: pkg-trans.arch
                .append("pkg-trans.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_TRANSCODER=${TARGET} \\")
                .append("\t-e PLUGIN_TRANSCODER_PREFIX=${TRANSCODER_PREFIX} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM=$(shell uname) \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM_ARCH=$(shell arch) \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BASE_IMAGE} \\")
                .append("\tbash /go/src/${PROJECT_NAME}/etc/script/package-transcoder.sh")
                .append2("\t@echo \"package transcoder ${TARGET} complete\"")

                // target: pkg-trace.arch
                .append("pkg-trace.arch:")
                .append("\tmkdir -p /tmp/go-build-cache")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_TRACE=${TARGET} \\")
                .append("\t-e PLUGIN_TRACE_PREFIX=${TRACE_PREFIX} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM=$(shell uname) \\")
                .append("\t-e PLUGIN_BUILD_PLATFORM_ARCH=$(shell arch) \\")
                .append("\t-e PLUGIN_GIT_VERSION=${GIT_VERSION} \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-v /tmp/go-build-cache:/root/.cache/go-build \\")
                .append("\t-v $(shell go env GOPATH)/pkg/mod/:/go/pkg/mod/ \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BASE_IMAGE} \\")
                .append("\tbash /go/src/${PROJECT_NAME}/etc/script/package-trace.sh")
                .append2("\t@echo \"package trace ${TARGET} complete\"")

                // target: pkg-ant.arch
                .append("pkg-ant.arch:")
                .append("\tdocker run --rm ${DOCKER_BUILD_OPTS} \\")
                .append("\t-e PLUGIN_OS=${PLUGIN_GOOS} \\")
                .append("\t-e PLUGIN_ARCH=${PLUGIN_GOARCH} \\")
                .append("\t-e PLUGIN_PROJECT_NAME=${PROJECT_NAME} \\")
                .append("\t-v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("\t-w /go/src/${PROJECT_NAME} ${BASE_IMAGE} \\")
                .append2("\tbash /go/src/${PROJECT_NAME}/etc/script/package-ant.sh")

                .append(".PHONY: codec clean start stop debug");

        Content = buffer.toString();
    }

    public static Source header() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(header());
    }
}