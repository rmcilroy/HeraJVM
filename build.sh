set -e

ant -Dconfig.name=prototype -Dhost.name=ia32-linux -Dtarget.name=ppc32-linux cross-compile-host
ant -Dconfig.name=prototype -Dhost.name=ppc32-linux cross-compile-target