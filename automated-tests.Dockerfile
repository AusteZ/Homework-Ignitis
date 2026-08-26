FROM gradle:jdk25

WORKDIR /workspace

COPY --chown=gradle:gradle . .

ENV AUTOMATED_TEST_BASE_URL=http://host.docker.internal:8080
ENV WAIT_FOR_HOST=host.docker.internal
ENV WAIT_FOR_PORT=8080

CMD ["bash", "-lc", "until timeout 2 bash -c \"</dev/tcp/${WAIT_FOR_HOST}/${WAIT_FOR_PORT}\"; do sleep 2; done; gradle :automated-tests:test --no-daemon"]
