
.PHONY: all clean run

all: build/exec

clean:
	rm -rf build

run: build/exec
	pxe build/exec

build/exec: build/exec.asm
	@mkdir -p build
	px16-as -o $@ $<

build/exec.asm: src/badapple.px16 output/bitstream.px16
	@mkdir -p build
	cat $^ > $@
