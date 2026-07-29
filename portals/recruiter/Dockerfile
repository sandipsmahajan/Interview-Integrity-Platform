FROM node:22-alpine AS build
WORKDIR /app
COPY portals/package.json ./package.json
COPY portals/recruiter ./recruiter
RUN npm install --workspace @interview-integrity/recruiter
RUN npm run build --workspace @interview-integrity/recruiter

FROM nginx:1.27-alpine
COPY --from=build /app/recruiter/dist /usr/share/nginx/html
