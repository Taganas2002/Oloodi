#!/bin/sh
# Render Postgres connectionString is postgresql://…; Spring wants jdbc:postgresql://…
set -e
if [ -z "${DB_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    jdbc:*)
      DB_URL="$DATABASE_URL"
      ;;
    postgres://*)
      DB_URL="jdbc:postgresql://${DATABASE_URL#postgres://}"
      ;;
    postgresql://*)
      DB_URL="jdbc:${DATABASE_URL}"
      ;;
    *)
      DB_URL="jdbc:postgresql://${DATABASE_URL}"
      ;;
  esac
  export DB_URL
fi
exec java -jar /app/app.jar